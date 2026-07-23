package com.shixiaoyu.xiangyueproject.service.Impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.config.GaodeProperties;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserCommentDTO;
import com.shixiaoyu.xiangyueproject.entity.po.*;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.LCCVO;
import com.shixiaoyu.xiangyueproject.entity.vo.LocationVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.mapper.*;
import com.shixiaoyu.xiangyueproject.service.UserService;
import com.shixiaoyu.xiangyueproject.util.LockUtils;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.AMAP_IP_API;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(GaodeProperties.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserMapper userMapper;
    private final GaodeProperties gaodeProperties;
    private final RestTemplate restTemplate;
    private final UserCollectMapper userCollectMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserCommentMapper userCommentMapper;
    private final ScenicMapper scenicMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final LockUtils lockUtils;
    private final RestHighLevelClient restHighLevelClient;
    private static final String ES_INDEX = "village";

    @Override
    public Result comment(UserCommentDTO userCommentDTO) {
        // 1. 从 UserHolder 获取当前登录用户 ID (安全、可靠)
        Long userId = UserHolder.getUser().getId();
        stringRedisTemplate.delete(USER_COMMENTS+userId);
        stringRedisTemplate.delete(SCENIC_COMMENTS+userCommentDTO.getTargetId());
        if (userId == null) {
            return Result.error("登录已过期，请重新登录");
        }

        // 2. 补全 DTO 里的 userId
        userCommentDTO.setUserId(userId);
        log.info("userCommentDTO:{}", userCommentDTO);

        // 3. 设置默认值（如果前端没传）
        if (userCommentDTO.getIsShow() == null) {
            userCommentDTO.setIsShow(1); // 默认展示
        }
        if (userCommentDTO.getTargetType() == null) {
            userCommentDTO.setTargetType(1); // 默认评论景点
        }

        log.info("用户 {} 正在对目标 {} 发表评价，评分: {}", userId, userCommentDTO.getTargetId(), userCommentDTO.getScore());

        // 4. 调用 Mapper 插入
        userMapper.comment(userCommentDTO);
        return Result.ok(UserHolder.getUser().getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result like(Long id) {
        // 1. 从 UserHolder 获取当前登录用户的 ID
        Long userId = UserHolder.getUser().getId();
        stringRedisTemplate.delete(USER_LIKES+userId);

        // 安全检查
        if (userId == null) {
            return Result.error("登录已过期，请重新登录");
        }

        // 2. 检查该用户是否点过赞 (这里必须带上 userId)
        Integer count = userMapper.checkLike(userId, id);

        if (count > 0) {
            // --- 取消点赞 ---
            userMapper.deleteLike(userId, id); // 必须根据 userId 和 targetId 删除
            userMapper.updateScenicLikeCount(id, -1);
            return Result.ok("已取消点赞");
        } else {
            // --- 点赞流程 ---
            UserLike userLike = new UserLike();
            userLike.setUserId(userId);    // 【核心修复点】：填充用户ID
            userLike.setTargetId(id);      // 填充景点ID
            userLike.setCreateTime(LocalDateTime.now()); // 填充时间

            // 3. 执行插入
            userMapper.insertLike(userLike);

            // 4. 更新景点表的 likes 数量
            userMapper.updateScenicLikeCount(id, 1);
            return Result.ok(UserHolder.getUser().getUsername());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及两张表的增删改，必须加事务
    public Result collect(Long id) {
        // 1. 获取当前登录用户 ID
        Long userId = UserHolder.getUser().getId();
        stringRedisTemplate.delete(USER_COLLECTIONS+userId);
        if (userId == null) {
            return Result.error("未登录或登录已过期");
        }

        // 2. 检查是否已经收藏过
        Integer count = userMapper.checkCollect(userId, id);

        if (count > 0) {
            // --- 取消收藏流程 ---
            userMapper.deleteCollect(userId, id);
            userMapper.updateScenicCollectCount(id, -1);

            log.info("用户 {} 取消收藏景点 {}", userId, id);
            return Result.ok("已取消收藏");
        } else {
            // --- 新增收藏流程 ---
            UserCollect userCollect = new UserCollect();
            userCollect.setUserId(userId);
            userCollect.setTargetId(id);
            // 手动设置当前时间
            userCollect.setCreateTime(LocalDateTime.now());

            userMapper.insertCollect(userCollect);
            userMapper.updateScenicCollectCount(id, 1);

            log.info("用户 {} 收藏景点成功", userId);
            stringRedisTemplate.delete(USER_COLLECTIONS+userId);
            return Result.ok(UserHolder.getUser().getUsername());
        }
    }

    @Override
    public Result<LocationVO> location(String ip) {
        ip = "111.53.227.84";//todo：测试用
        String apiKey = gaodeProperties.getApiKey();
        String url = AMAP_IP_API + "?key=" + apiKey + "&ip=" + ip;
        GaodeIP res = restTemplate.getForObject(url, GaodeIP.class);
        if (res == null) {
            return Result.error("获取位置信息失败");
        }
        if (res.getProvince() == null || res.getCity() == null) {
            return Result.ok(new LocationVO("未知省", "未知市"));
        }
        String realProcince = getReal(res.getProvince());
        String realCity = getReal(res.getCity());
        return Result.ok(new LocationVO(realProcince, realCity));
    }

    @Override
    public Result<List<ScenicVO>> getLikes() {
        return Result.ok(getVOList(userLikeMapper,
                UserHolder.getUser().getId(),
                UserLike::getUserId,
                UserLike::getTargetId,
                UserLike::getCreateTime,
                RedisConstants.USER_LIKES,
                RedisConstants.LOCK_LIKE_PREFIX));
    }

    @Override
    public Result<List<ScenicVO>> getComments() {
        return Result.ok(getVOList(userCommentMapper,
                UserHolder.getUser().getId(),
                UserComment::getUserId,
                UserComment::getTargetId,
                UserComment::getCreateTime,
                RedisConstants.USER_COMMENTS,
                RedisConstants.LOCK_COMMENT_PREFIX));
    }

    @Override
    public Result<List<ScenicVO>> getCollections() {
        return Result.ok(getVOList(userCollectMapper, UserHolder.getUser().getId(),
                UserCollect::getUserId,
                UserCollect::getTargetId,
                UserCollect::getCreateTime,
                RedisConstants.USER_COLLECTIONS,
                RedisConstants.LOCK_COLLECT_PREFIX));
    }

    @Override
    public Result isLike(Long scenicId) {
        LambdaQueryWrapper<UserLike> queryWrapper = new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getTargetId, scenicId)
                .eq(UserLike::getUserId, UserHolder.getUser().getId());
        UserLike userLike = userLikeMapper.selectOne(queryWrapper);
        if(userLike==null){
            log.info("用户 {} 未点赞景点 {}", UserHolder.getUser().getId(), scenicId);
            return Result.ok(0);
        }
        log.info("用户 {} 已点赞景点 {}", UserHolder.getUser().getId(), scenicId);
        return Result.ok(1);
    }

    @Override
    public Result isCollect(Long scenicId) {
        LambdaQueryWrapper<UserCollect> queryWrapper = new LambdaQueryWrapper<UserCollect>()
                .eq(UserCollect::getTargetId, scenicId)
                .eq(UserCollect::getUserId, UserHolder.getUser().getId());
        UserCollect userCollect = userCollectMapper.selectOne(queryWrapper);
        log.info("userCollect:{}",userCollect);
        if(userCollect==null){
            log.info("用户 {} 未收藏景点 {}", UserHolder.getUser().getId(), scenicId);
            return Result.ok(0);
        }
        log.info("用户 {} 已收藏景点 {}", UserHolder.getUser().getId(), scenicId);
        return Result.ok(1);
    }

    @Override
    public Result<List<VillageBaseVO>> search(String content) {
        List<VillageBaseVO> resultList = new ArrayList<>();

        try {
            // 1. 创建搜索请求
            SearchRequest searchRequest = new SearchRequest(ES_INDEX);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            // 2. 关键词搜索（多字段模糊匹配）
            if (StringUtils.hasText(content)) {
                sourceBuilder.query(QueryBuilders.multiMatchQuery(content,
                        "name",        // 村落名
                        "intro",       // 介绍
                        "activity",    // 活动
                        "province",    // 省
                        "city",        // 市
                        "county",      // 县
                        "managerName"  // 村长姓名
                ));
            } else {
                // 无关键词：返回全部
                sourceBuilder.query(QueryBuilders.matchAllQuery());
            }

            // 3. 执行搜索
            searchRequest.source(sourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();

            // 4. 解析结果 → 封装成 VillageBaseVO
            for (SearchHit hit : hits) {
                // JSON 直接转实体，干净无高亮
                VillageBaseVO vo = JSONUtil.toBean(hit.getSourceAsString(), VillageBaseVO.class);
                resultList.add(vo);
            }

        } catch (Exception e) {
            log.error("搜索服务异常");
            return Result.ok(new ArrayList<>());
        }

        return Result.ok(resultList);
    }


    /**
     * 统一泛型方法+解决缓存击穿、雪崩、穿透问题
     * @param mapper
     * @param id
     * @param userIdGetter
     * @param targetIdGetter
     * @param timeGetter
     * @param cachePrefix
     * @param lockPrefix
     * @return
     * @param <T>
     */
    private <T> List<ScenicVO> getVOList(BaseMapper<T> mapper,
                                         Long id,
                                         SFunction<T, Long> userIdGetter,
                                         Function<T, Long> targetIdGetter,
                                         SFunction<T, LocalDateTime> timeGetter,
                                         String cachePrefix,
                                         String lockPrefix) {
        String cacheKey = cachePrefix +id;
        String lockKey= lockPrefix + id;
        int retry=0;
        while(retry<MAX_RETRY_COUNT){
            //查缓存
            String cacheRes=stringRedisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(cacheRes)) {
                List<ScenicVO> list = JSONUtil.toList(cacheRes, ScenicVO.class);
                log.info("用户 {} 获取的景点列表：{}", id, list);
                return list;
            }
            //缓存为空，尝试获取锁并进行缓存重建
            boolean tryLock = lockUtils.tryLock(lockKey);
            if(tryLock){
                try{
                    //拿到锁，进行double check，查看在这期间是否有拿到缓存
                    cacheRes = stringRedisTemplate.opsForValue().get(cacheKey);
                    if (StrUtil.isNotBlank(cacheRes)) {
                        List<ScenicVO> list = JSONUtil.toList(cacheRes, ScenicVO.class);
                        log.info("用户 {} 获取的景点列表：{}", id, list);
                        return list;
                    }
                    //发现没有缓存，开始重建缓存，查询数据库
                    return reBuildCache(mapper, id, userIdGetter, targetIdGetter, timeGetter, cacheKey);
                } finally {
                    //使用finally，保证锁一定会被释放
                    lockUtils.unlock(lockKey);
                }
            }
            else{
                try {
                    Thread.sleep(RETRY_TIME);
                } catch (InterruptedException e) {
                    log.error("线程重试被中断,{}",e.getMessage());
                    return Collections.emptyList();
                }
                retry++;
            }
        }
        log.info("用户 {} 尝试获取锁失败，重试次数：{}", id, retry);
        log.warn("缓存重建失败");
        return Collections.emptyList();
    }

    private <T> @NotNull List<ScenicVO> reBuildCache(BaseMapper<T> mapper, Long id, SFunction<T, Long> userIdGetter, Function<T, Long> targetIdGetter, SFunction<T, LocalDateTime> timeGetter, String cacheKey) {
        LambdaQueryWrapper<T> query1 = new LambdaQueryWrapper<T>().eq(userIdGetter, id).orderByDesc(timeGetter);
        List<T> userLikes = mapper.selectList(query1);
        List<Long> scenicIdList = userLikes.stream().map(targetIdGetter).toList();
        log.info("用户 {} 获取的景点 ID 列表：{}", id, scenicIdList);
        if (scenicIdList.isEmpty()) {
            int ttl = RandomUtil.randomInt(0, 401) + USER_COMMENTS_TTL;
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(Collections.emptyList()), ttl, TimeUnit.SECONDS);
            return Collections.emptyList();
        }
        String join = StrUtil.join(",", scenicIdList);
        LambdaQueryWrapper<VillageScenic> scenicQuery = new LambdaQueryWrapper<VillageScenic>().in(VillageScenic::getId, scenicIdList).last("order by field(id," + join + ")");
        List<VillageScenic> villageScenics = scenicMapper.selectList(scenicQuery);
        List<ScenicVO> voList = villageScenics.stream().map(item -> BeanUtil.copyProperties(item, ScenicVO.class)).toList();
        int ttl = RandomUtil.randomInt(0, 401) + USER_COMMENTS_TTL;
        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(voList), ttl, TimeUnit.SECONDS);
        return voList;
    }

    private String getReal(Object province) {
        String real = "";
        if (province instanceof List<?> list) {
            if (CollectionUtils.isNotEmpty(list)) {
                real = list.get(0).toString();
            }
        } else {
            real = province.toString();
        }
        return real;
    }
}

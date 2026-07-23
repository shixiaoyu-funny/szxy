package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.ScenicAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.po.*;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserCommentVO;
import com.shixiaoyu.xiangyueproject.enums.FarmerTypeEnum;
import com.shixiaoyu.xiangyueproject.enums.ReviewStatusEnum;
import com.shixiaoyu.xiangyueproject.enums.UserTypeEnum;
import com.shixiaoyu.xiangyueproject.mapper.*;
import com.shixiaoyu.xiangyueproject.server.WebSocketServer;
import com.shixiaoyu.xiangyueproject.service.ScenicService;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.bcel.Const;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.SCENIC_COMMENTS;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.USER_COMMENTS_TTL;

/**
 * 景点服务实现类
 */
@Slf4j
@Service
public class ScenicServiceImpl extends ServiceImpl<ScenicAccessMapper, ScenicAccess> implements ScenicService {

    @Resource
    private FarmerMapper farmerMapper;

    @Resource
    private ScenicAccessMapper scenicAccessMapper;

    @Resource
    private HttpServletRequest request;
    @Resource
    private VillageScenicMapper villageScenicMapper;
    @Resource
    private ScenicMapper scenicMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserCommentMapper userCommentMapper;
    @Resource
    private VillageService villageService;
    @Resource
    private VillageMapper villageMapper;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public Result registerPrivateScenic(ScenicAccessDTO dto) {
        // 1. 权限校验：从请求属性获取当前用户ID
        Long userId = (Long) request.getAttribute(CommonConstants.ATTR_USER_ID);
        FarmerUser farmer = farmerMapper.selectOne(
                new LambdaQueryWrapper<FarmerUser>()
                        .eq(FarmerUser::getUserId, userId)
        );
        // DTO 字段名为 villageName，实际传村落主键 ID（与前端约定一致）
        Long villageId = dto.getVillageName();
        if (villageId == null) {
            return Result.error("请选择所属村落");
        }
        LambdaQueryWrapper<VillageBase> wrapper = new LambdaQueryWrapper<VillageBase>().eq(VillageBase::getId, villageId);
        VillageBase villageBase = villageMapper.selectOne(wrapper);
        if (villageBase == null) {
            return Result.error("该村落不存在");
        }
        //转换实体
        ScenicAccess access = new ScenicAccess();
        BeanUtils.copyProperties(dto, access);
        access.setVillageId(villageBase.getId());
        // 3. 设置初始状态：待审核 (ReviewStatusEnum.UNDER_REVIEW = 0)
        access.setStatus(ReviewStatusEnum.UNDER_REVIEW.getCode());
        access.setUserId(userId); // 记录是谁申请的
        webSocketServer.sendToAllClient("scenic_access");
        return this.save(access) ? Result.ok() : Result.error("申请失败");
    }

    @Override
    public Result registerPublicScenic(ScenicAccessDTO dto) {
        // 1. 权限校验：从请求属性获取当前用户ID
        Long userId = (Long) request.getAttribute(CommonConstants.ATTR_USER_ID);
        FarmerUser farmer = farmerMapper.selectOne(
                new LambdaQueryWrapper<FarmerUser>()
                        .eq(FarmerUser::getUserId, userId)
        );
        // 校验是否为村长 (FarmerTypeEnum.VILLAGE_MANAGER = 2)
        if (farmer == null || !farmer.getType().equals(FarmerTypeEnum.VILLAGE_MANAGER.getCode())) {
            return Result.error("只有村长身份可以申请公共景点");
        }
        Long villageId = dto.getVillageName();
        LambdaQueryWrapper<VillageBase> wrapper = new LambdaQueryWrapper<VillageBase>().eq(VillageBase::getId, villageId);
        VillageBase villageBase = villageMapper.selectOne(wrapper);
        if (villageBase == null) {
            return Result.error("该村落不存在");
        }
        //转换实体
        ScenicAccess access = new ScenicAccess();
        BeanUtils.copyProperties(dto, access);
        access.setVillageId(villageBase.getId());
        // 3. 设置初始状态：待审核 (ReviewStatusEnum.UNDER_REVIEW = 0)
        access.setStatus(ReviewStatusEnum.UNDER_REVIEW.getCode());
        access.setUserId(userId); // 记录是谁申请的
        webSocketServer.sendToAllClient("scenic_access");
        return this.save(access) ? Result.ok() : Result.error("申请失败");
    }

    @Override
    public Result getScenicAccessList(){
        List<ScenicAccess> list = scenicAccessMapper.selectList(null);
        return Result.ok(list);
    }

    @Override
    public Result<List<ScenicVO>> getTop10Scenic() {
        LambdaQueryWrapper<VillageScenic> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(VillageScenic::getLikes) // 根据点赞量降序
                .last("limit 10");                    // 只取前10条
        // 2. 查询数据库
        List<VillageScenic> list = villageScenicMapper.selectList(wrapper);

        // 3. 将 Entity 转换为 VO（如果字段不一致需要拷贝）
        List<ScenicVO> voList = list.stream().map(scenic -> {
            ScenicVO vo = new ScenicVO();
            BeanUtils.copyProperties(scenic, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.ok(voList);
    }

    @Override
    public List<UserCommentVO> getScComments(Long id) {
        String key=SCENIC_COMMENTS+id;
        String res = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(res)){
            log.info("从缓存中获取景点 {} 的评论",id);
            return JSONUtil.toList(res,UserCommentVO.class);
        }

        LambdaQueryWrapper<UserComment> wrapper = new LambdaQueryWrapper<UserComment>()
                .eq(UserComment::getTargetId, id)
                .orderByDesc(UserComment::getCreateTime);
        List<UserComment> userComments = userCommentMapper.selectList(wrapper);
        if(userComments.isEmpty()){
            int randomTtl = RandomUtil.randomInt(0, 401) + USER_COMMENTS_TTL;
            stringRedisTemplate.opsForValue().set(
                    key,
                    JSONUtil.toJsonStr(Collections.emptyList()),
                    randomTtl,
                    TimeUnit.SECONDS
            );
            return Collections.emptyList();
        }
        List<UserCommentVO> list = userComments.stream().map(userComment -> {
            UserCommentVO userCommentVO = BeanUtil.copyProperties(userComment, UserCommentVO.class);
            userCommentVO.setUsername(UserHolder.getUser().getUsername());
            return userCommentVO;
        }).toList();
        log.info("从数据库中获取评论:{}",list);
        //  数据库有数据 → 缓存数据 + 随机TTL（防雪崩）
        int randomTtl = RandomUtil.randomInt(0, 401) + USER_COMMENTS_TTL;
        stringRedisTemplate.opsForValue().set(
                key,
                JSONUtil.toJsonStr(list),
                randomTtl,
                TimeUnit.SECONDS
        );
        return list;
    }

    @Override
    public Result<ScenicVO> detail(Long id) {
        VillageScenic villageScenic = scenicMapper.selectById(id);
        Long villageId = villageScenic.getVillageId();
        VillageBase villageBase = villageService.getById(villageId);
        ScenicVO scenicVO = BeanUtil.copyProperties(villageScenic, ScenicVO.class);
        scenicVO.setVillageName(villageBase.getName());
        log.info("查询的景点信息为：{}",villageScenic);
        return Result.ok(scenicVO);
    }
}

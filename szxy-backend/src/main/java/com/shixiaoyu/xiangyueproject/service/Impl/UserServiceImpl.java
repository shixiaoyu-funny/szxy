package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserCommentDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.UserQueryDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.UserCollect;
import com.shixiaoyu.xiangyueproject.entity.po.UserComment;
import com.shixiaoyu.xiangyueproject.entity.po.UserLike;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.vo.AdminUserVO;
import com.shixiaoyu.xiangyueproject.entity.vo.LikeReceivedVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.enums.CommentShowEnum;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserCollectMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserCommentMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserLikeMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.UserService;
import com.shixiaoyu.xiangyueproject.service.support.ScenicVoFiller;
import com.shixiaoyu.xiangyueproject.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.VILLAGE_COLLECTIONS_TOP_10;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.VILLAGE_LIKES_TOP_10;

/**
 * 用户端服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserCollectMapper userCollectMapper;
    private final UserCommentMapper userCommentMapper;
    private final ScenicMapper scenicMapper;
    private final VillageMapper villageMapper;
    private final FarmerMapper farmerMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ScenicVoFiller scenicVoFiller;

    /**
     * 用户评论景点（支持楼中楼；回复不允许带图）
     *
     * @param dto 评论内容
     */
    @Override
    public void comment(UserCommentDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        if (scenicMapper.selectById(dto.getScenicId()) == null) {
            throw new BusinessException("景点不存在");
        }
        if (dto.getParentId() != null) {
            UserComment parent = userCommentMapper.selectById(dto.getParentId());
            if (parent == null || !dto.getScenicId().equals(parent.getScenicId())) {
                throw new BusinessException("父评论不存在或不属于该景点");
            }
            // 楼中楼不允许带图
            dto.setCommentImg(null);
        }
        UserComment comment = BeanUtil.copyProperties(dto, UserComment.class);
        comment.setUserId(userId);
        if (dto.getParentId() != null) {
            comment.setCommentImg(null);
        }
        if (comment.getIsShow() == null) {
            comment.setIsShow(CommentShowEnum.SHOW);
        }
        userCommentMapper.insert(comment);
        stringRedisTemplate.delete(RedisConstants.USER_COMMENTS + userId);
        stringRedisTemplate.delete(RedisConstants.SCENIC_COMMENTS + dto.getScenicId());
    }

    /**
     * 点赞/取消点赞（幂等切换 + 景点计数增减 + 失效相关缓存）
     *
     * @param scenicId 景点ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String like(Long scenicId) {
        if (scenicMapper.selectById(scenicId) == null) {
            throw new BusinessException("景点不存在");
        }
        Long userId = SecurityUtil.currentUserId();
        Long count = userLikeMapper.selectCount(
                new LambdaQueryWrapper<UserLike>().eq(UserLike::getUserId, userId).eq(UserLike::getScenicId, scenicId));
        boolean liked = count != null && count > 0;
        if (liked) {
            userLikeMapper.delete(new LambdaQueryWrapper<UserLike>()
                    .eq(UserLike::getUserId, userId).eq(UserLike::getScenicId, scenicId));
            updateScenicCount(scenicId, "likes", -1);
        } else {
            UserLike like = new UserLike();
            like.setUserId(userId);
            like.setScenicId(scenicId);
            userLikeMapper.insert(like);
            updateScenicCount(scenicId, "likes", 1);
        }
        stringRedisTemplate.delete(RedisConstants.USER_LIKES + userId);
        stringRedisTemplate.delete(VILLAGE_LIKES_TOP_10);
        return liked ? "已取消点赞" : "点赞成功";
    }

    /**
     * 收藏/取消收藏（幂等切换 + 景点计数增减 + 失效相关缓存）
     *
     * @param scenicId 景点ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String collect(Long scenicId) {
        if (scenicMapper.selectById(scenicId) == null) {
            throw new BusinessException("景点不存在");
        }
        Long userId = SecurityUtil.currentUserId();
        Long count = userCollectMapper.selectCount(
                new LambdaQueryWrapper<UserCollect>().eq(UserCollect::getUserId, userId).eq(UserCollect::getScenicId, scenicId));
        boolean collected = count != null && count > 0;
        if (collected) {
            userCollectMapper.delete(new LambdaQueryWrapper<UserCollect>()
                    .eq(UserCollect::getUserId, userId).eq(UserCollect::getScenicId, scenicId));
            updateScenicCount(scenicId, "collections", -1);
        } else {
            UserCollect collect = new UserCollect();
            collect.setUserId(userId);
            collect.setScenicId(scenicId);
            userCollectMapper.insert(collect);
            updateScenicCount(scenicId, "collections", 1);
        }
        stringRedisTemplate.delete(RedisConstants.USER_COLLECTIONS + userId);
        stringRedisTemplate.delete(VILLAGE_COLLECTIONS_TOP_10);
        return collected ? "已取消收藏" : "收藏成功";
    }

    /** 当前用户是否已点赞该景点 */
    @Override
    public boolean isLike(Long scenicId) {
        Long count = userLikeMapper.selectCount(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, SecurityUtil.currentUserId()).eq(UserLike::getScenicId, scenicId));
        return count != null && count > 0;
    }

    /** 当前用户是否已收藏该景点 */
    @Override
    public boolean isCollect(Long scenicId) {
        Long count = userCollectMapper.selectCount(new LambdaQueryWrapper<UserCollect>()
                .eq(UserCollect::getUserId, SecurityUtil.currentUserId()).eq(UserCollect::getScenicId, scenicId));
        return count != null && count > 0;
    }

    /** 用户历史点赞的景点列表（带 Redis 缓存） */
    @Override
    public List<ScenicVO> getLikes() {
        return listRelatedScenics(userLikeMapper, SecurityUtil.currentUserId(),
                UserLike::getUserId, UserLike::getScenicId, UserLike::getCreateTime, RedisConstants.USER_LIKES);
    }

    /** 用户历史评论过的景点列表（带 Redis 缓存） */
    @Override
    public List<ScenicVO> getComments() {
        return listRelatedScenics(userCommentMapper, SecurityUtil.currentUserId(),
                UserComment::getUserId, UserComment::getScenicId, UserComment::getCreateTime, RedisConstants.USER_COMMENTS);
    }

    /** 用户历史收藏的景点列表（带 Redis 缓存） */
    @Override
    public List<ScenicVO> getCollections() {
        return listRelatedScenics(userCollectMapper, SecurityUtil.currentUserId(),
                UserCollect::getUserId, UserCollect::getScenicId, UserCollect::getCreateTime, RedisConstants.USER_COLLECTIONS);
    }

    /**
     * 农村模糊搜索（名称/介绍/省市区/活动）+ 分页
     *
     * @param content  关键词，可空
     * @param pageNo   页码
     * @param pageSize 每页条数
     */
    @Override
    public PageResultVO<VillageBaseVO> villageSearch(String content, Integer pageNo, Integer pageSize) {
        int pn = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<VillageBase> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(content)) {
            String kw = StrUtil.trim(content);
            wrapper.and(w -> w.like(VillageBase::getName, kw)
                    .or().like(VillageBase::getIntro, kw)
                    .or().like(VillageBase::getProvince, kw)
                    .or().like(VillageBase::getCity, kw)
                    .or().like(VillageBase::getCounty, kw)
                    .or().like(VillageBase::getActivity, kw));
        }
        wrapper.orderByDesc(VillageBase::getCreateTime);
        Page<VillageBase> page = villageMapper.selectPage(Page.of(pn, ps), wrapper);
        List<VillageBaseVO> voList = page.getRecords().stream()
                .map(po -> BeanUtil.copyProperties(po, VillageBaseVO.class))
                .collect(Collectors.toList());
        fillManagerNames(voList, page.getRecords());
        return new PageResultVO<>(page.getTotal(), voList);
    }

    /**
     * 景点模糊搜索（名称/介绍/住宿 + 所属村名）+ 分页
     *
     * @param content  关键词，可空
     * @param pageNo   页码
     * @param pageSize 每页条数
     */
    @Override
    public PageResultVO<ScenicVO> scenicSearch(String content, Integer pageNo, Integer pageSize) {
        int pn = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LambdaQueryWrapper<VillageScenic> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(content)) {
            String kw = StrUtil.trim(content);
            List<Long> villageIds = villageMapper.selectList(new LambdaQueryWrapper<VillageBase>()
                            .like(VillageBase::getName, kw))
                    .stream()
                    .map(VillageBase::getId)
                    .toList();
            wrapper.and(w -> {
                w.like(VillageScenic::getName, kw)
                        .or().like(VillageScenic::getIntro, kw)
                        .or().like(VillageScenic::getAccommodationInfo, kw);
                if (!villageIds.isEmpty()) {
                    w.or().in(VillageScenic::getVillageId, villageIds);
                }
            });
        }
        wrapper.orderByDesc(VillageScenic::getCreateTime);
        Page<VillageScenic> page = scenicMapper.selectPage(Page.of(pn, ps), wrapper);
        List<ScenicVO> voList = page.getRecords().stream()
                .map(po -> BeanUtil.copyProperties(po, ScenicVO.class))
                .collect(Collectors.toList());
        fillScenicVillageNames(voList);
        scenicVoFiller.fillTicketPrice(voList);
        return new PageResultVO<>(page.getTotal(), voList);
    }

    /**
     * 收到的点赞：他人点赞我创建的景点
     */
    @Override
    public List<LikeReceivedVO> likesReceived() {
        Long me = SecurityUtil.currentUserId();
        // 1. 查我创建的全部景点
        List<VillageScenic> myScenics = scenicMapper.selectList(new LambdaQueryWrapper<VillageScenic>()
                .eq(VillageScenic::getUserId, me));
        if (myScenics.isEmpty()) {
            return List.of();
        }
        Map<Long, VillageScenic> scenicMap = myScenics.stream()
                .collect(Collectors.toMap(VillageScenic::getId, s -> s, (a, b) -> a));
        // 2. 查他人对我景点的点赞（最近 100 条，排除自己）
        List<UserLike> likes = userLikeMapper.selectList(new LambdaQueryWrapper<UserLike>()
                .in(UserLike::getScenicId, scenicMap.keySet())
                .ne(UserLike::getUserId, me)
                .orderByDesc(UserLike::getCreateTime)
                .last("limit 100"));
        if (likes.isEmpty()) {
            return List.of();
        }
        // 3. 批量查点赞人信息
        Set<Long> likerIds = likes.stream().map(UserLike::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = likerIds.isEmpty() ? Map.of()
                : userMapper.selectByIds(likerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 4. 组装消息动态 VO
        List<LikeReceivedVO> result = new ArrayList<>();
        for (UserLike like : likes) {
            User u = userMap.get(like.getUserId());
            VillageScenic scenic = scenicMap.get(like.getScenicId());
            LikeReceivedVO vo = new LikeReceivedVO();
            vo.setUsername(u != null ? u.getUsername() : "用户");
            vo.setAvatar(u != null ? u.getAvatar() : null);
            vo.setScenicId(like.getScenicId());
            vo.setScenicName(scenic != null ? scenic.getName() : "景点");
            vo.setActionText("赞了你的景点「" + vo.getScenicName() + "」");
            vo.setCreateTime(like.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    // ===================== 私有工具 =====================

    /**
     * 点赞/收藏数原子增减，避免并发丢失
     */
    private void updateScenicCount(Long scenicId, String column, int step) {
        scenicMapper.update(null, new LambdaUpdateWrapper<VillageScenic>()
                .eq(VillageScenic::getId, scenicId)
                .setSql(column + " = " + column + " + " + step));
    }

    /**
     * 通用：查用户关联的景点列表，先读 Redis，未命中再查库并写缓存
     *
     * @param mapper        点赞/收藏/评论 Mapper
     * @param userId        当前用户
     * @param cachePrefix   Redis 键前缀
     */
    private <T> List<ScenicVO> listRelatedScenics(BaseMapper<T> mapper, Long userId,
                                                  SFunction<T, Long> userIdGetter,
                                                  SFunction<T, Long> scenicIdGetter,
                                                  SFunction<T, LocalDateTime> timeGetter,
                                                  String cachePrefix) {
        String cacheKey = cachePrefix + userId;
        // 1. 先读 Redis 缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            try {
                return JSONUtil.toList(cached, ScenicVO.class);
            } catch (Exception e) {
                log.error("解析缓存失败，key:{}", cacheKey, e);
            }
        }
        // 2. 查用户关联记录（点赞/收藏/评论），按时间倒序
        LambdaQueryWrapper<T> qw = new LambdaQueryWrapper<T>()
                .eq(userIdGetter, userId).orderByDesc(timeGetter);
        List<T> rows = mapper.selectList(qw);
        // 3. 提取景点 ID，批量查景点详情（保持原顺序）
        List<Long> scenicIds = rows.stream().map(scenicIdGetter).filter(Objects::nonNull).distinct().toList();
        List<ScenicVO> result = new ArrayList<>();
        if (!scenicIds.isEmpty()) {
            Map<Long, VillageScenic> scenicMap = scenicMapper.selectList(
                            new LambdaQueryWrapper<VillageScenic>().in(VillageScenic::getId, scenicIds))
                    .stream().collect(Collectors.toMap(VillageScenic::getId, s -> s));
            for (Long id : scenicIds) {
                VillageScenic s = scenicMap.get(id);
                if (s != null) {
                    result.add(BeanUtil.copyProperties(s, ScenicVO.class));
                }
            }
            fillScenicVillageNames(result);
            scenicVoFiller.fillTicketPrice(result);
        }
        // 4. 写回 Redis（随机 TTL 防雪崩）
        int ttl = RandomUtil.randomInt(0, 401) + RedisConstants.USER_COMMENTS_TTL;
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(result), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("写缓存失败，key:{}", cacheKey, e);
        }
        return result;
    }

    /**
     * 景点转 VO 并批量填充村落名称，避免 N+1
     */
    private void fillScenicVillageNames(List<ScenicVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        // 1. 收集村落 ID
        Set<Long> villageIds = voList.stream().map(ScenicVO::getVillageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (villageIds.isEmpty()) {
            return;
        }
        // 2. 批量查村名，回填到 VO
        Map<Long, String> nameMap = villageMapper.selectByIds(villageIds).stream()
                .collect(Collectors.toMap(VillageBase::getId, VillageBase::getName, (a, b) -> a));
        voList.forEach(v -> v.setVillageName(nameMap.get(v.getVillageId())));
    }

    /**
     * 批量填充村长展示名
     */
    private void fillManagerNames(List<VillageBaseVO> voList, List<VillageBase> poList) {
        // 1. 收集村长 userId
        Set<Long> manageIds = poList.stream().map(VillageBase::getManageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (manageIds.isEmpty()) {
            return;
        }
        // 2. 批量查用户，回填村长名
        Map<Long, User> userMap = userMapper.selectByIds(manageIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        for (int i = 0; i < voList.size(); i++) {
            Long manageId = poList.get(i).getManageId();
            if (manageId == null) {
                continue;
            }
            User u = userMap.get(manageId);
            if (u != null) {
                voList.get(i).setManagerName(u.getUsername());
            }
        }
    }

    /**
     * 管理端分页查询用户（关键词/角色/状态筛选，不返回密码）
     *
     * @param queryDTO 查询条件
     */
    @Override
    public PageResultVO<AdminUserVO> adminList(UserQueryDTO queryDTO) {
        SecurityUtil.requireAdmin();
        int pageNo = queryDTO.getPageNo() == null || queryDTO.getPageNo() < 1 ? 1 : queryDTO.getPageNo();
        int pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : queryDTO.getPageSize();

        String keyword = StrUtil.trim(queryDTO.getKeyword());
        RoleEnum role = RoleEnum.fromCode(queryDTO.getRole());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(User::getUsername, keyword)
                        .or().like(User::getPhone, keyword)
                        .or().like(User::getEmail, keyword))
                .eq(role != null, User::getRole, role)
                .eq(queryDTO.getStatus() != null, User::getStatus, queryDTO.getStatus())
                .orderByDesc(User::getCreateTime);

        Page<User> page = userMapper.selectPage(Page.of(pageNo, pageSize), wrapper);
        List<AdminUserVO> voList = page.getRecords().stream()
                .map(user -> BeanUtil.copyProperties(user, AdminUserVO.class))
                .collect(Collectors.toList());
        return new PageResultVO<>(page.getTotal(), voList);
    }
}

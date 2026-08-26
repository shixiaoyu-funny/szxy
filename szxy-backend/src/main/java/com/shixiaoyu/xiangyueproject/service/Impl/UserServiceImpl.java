package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserCommentDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.UserCollect;
import com.shixiaoyu.xiangyueproject.entity.po.UserComment;
import com.shixiaoyu.xiangyueproject.entity.po.UserLike;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.vo.LikeReceivedVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.enums.CommentShowEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserCollectMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserCommentMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserLikeMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.UserService;
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
    private final ObjectMapper objectMapper;

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

    @Override
    public boolean isLike(Long scenicId) {
        Long count = userLikeMapper.selectCount(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, SecurityUtil.currentUserId()).eq(UserLike::getScenicId, scenicId));
        return count != null && count > 0;
    }

    @Override
    public boolean isCollect(Long scenicId) {
        Long count = userCollectMapper.selectCount(new LambdaQueryWrapper<UserCollect>()
                .eq(UserCollect::getUserId, SecurityUtil.currentUserId()).eq(UserCollect::getScenicId, scenicId));
        return count != null && count > 0;
    }

    @Override
    public List<ScenicVO> getLikes() {
        return listRelatedScenics(userLikeMapper, SecurityUtil.currentUserId(),
                UserLike::getUserId, UserLike::getScenicId, UserLike::getCreateTime, RedisConstants.USER_LIKES);
    }

    @Override
    public List<ScenicVO> getComments() {
        return listRelatedScenics(userCommentMapper, SecurityUtil.currentUserId(),
                UserComment::getUserId, UserComment::getScenicId, UserComment::getCreateTime, RedisConstants.USER_COMMENTS);
    }

    @Override
    public List<ScenicVO> getCollections() {
        return listRelatedScenics(userCollectMapper, SecurityUtil.currentUserId(),
                UserCollect::getUserId, UserCollect::getScenicId, UserCollect::getCreateTime, RedisConstants.USER_COLLECTIONS);
    }

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
        return new PageResultVO<>(page.getTotal(), voList);
    }

    /**
     * 收到的点赞：他人点赞我创建的景点
     */
    @Override
    public List<LikeReceivedVO> likesReceived() {
        Long me = SecurityUtil.currentUserId();
        List<VillageScenic> myScenics = scenicMapper.selectList(new LambdaQueryWrapper<VillageScenic>()
                .eq(VillageScenic::getUserId, me));
        if (myScenics.isEmpty()) {
            return List.of();
        }
        Map<Long, VillageScenic> scenicMap = myScenics.stream()
                .collect(Collectors.toMap(VillageScenic::getId, s -> s, (a, b) -> a));
        List<UserLike> likes = userLikeMapper.selectList(new LambdaQueryWrapper<UserLike>()
                .in(UserLike::getScenicId, scenicMap.keySet())
                .ne(UserLike::getUserId, me)
                .orderByDesc(UserLike::getCreateTime)
                .last("limit 100"));
        if (likes.isEmpty()) {
            return List.of();
        }
        Set<Long> likerIds = likes.stream().map(UserLike::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = likerIds.isEmpty() ? Map.of()
                : userMapper.selectByIds(likerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

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
     * 查询用户关联的景点列表（带缓存，随机TTL防雪崩）
     */
    private <T> List<ScenicVO> listRelatedScenics(BaseMapper<T> mapper, Long userId,
                                                  SFunction<T, Long> userIdGetter,
                                                  SFunction<T, Long> scenicIdGetter,
                                                  SFunction<T, LocalDateTime> timeGetter,
                                                  String cachePrefix) {
        String cacheKey = cachePrefix + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<ScenicVO>>() {
                });
            } catch (Exception e) {
                log.error("解析缓存失败，key:{}", cacheKey, e);
            }
        }
        LambdaQueryWrapper<T> qw = new LambdaQueryWrapper<T>()
                .eq(userIdGetter, userId).orderByDesc(timeGetter);
        List<T> rows = mapper.selectList(qw);
        List<Long> scenicIds = rows.stream().map(scenicIdGetter).filter(Objects::nonNull).distinct().toList();
        List<ScenicVO> result = new ArrayList<>();
        if (!scenicIds.isEmpty()) {
            Map<Long, VillageScenic> scenicMap = scenicMapper.selectList(
                            new LambdaQueryWrapper<VillageScenic>().in(VillageScenic::getId, scenicIds))
                    .stream().collect(Collectors.toMap(VillageScenic::getId, s -> s));
            for (Long id : scenicIds) {
                VillageScenic s = scenicMap.get(id);
                if (s != null) {
                    result.add(toScenicVO(s));
                }
            }
        }
        int ttl = RandomUtil.randomInt(0, 401) + RedisConstants.USER_COMMENTS_TTL;
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), ttl, TimeUnit.SECONDS);
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
        Set<Long> villageIds = voList.stream().map(ScenicVO::getVillageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (villageIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = villageMapper.selectByIds(villageIds).stream()
                .collect(Collectors.toMap(VillageBase::getId, VillageBase::getName, (a, b) -> a));
        voList.forEach(v -> v.setVillageName(nameMap.get(v.getVillageId())));
    }

    private ScenicVO toScenicVO(VillageScenic scenic) {
        ScenicVO vo = BeanUtil.copyProperties(scenic, ScenicVO.class);
        fillScenicVillageNames(List.of(vo));
        return vo;
    }

    /**
     * 批量填充村长展示名
     */
    private void fillManagerNames(List<VillageBaseVO> voList, List<VillageBase> poList) {
        Set<Long> manageIds = poList.stream().map(VillageBase::getManageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (manageIds.isEmpty()) {
            return;
        }
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
}

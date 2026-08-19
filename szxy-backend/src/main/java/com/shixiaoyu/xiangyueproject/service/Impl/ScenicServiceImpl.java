package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixiaoyu.xiangyueproject.constants.ErrorConstants;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.ScenicDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.UserComment;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserCommentVO;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserCommentMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.ScenicService;
import com.shixiaoyu.xiangyueproject.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 景点服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScenicServiceImpl extends ServiceImpl<ScenicMapper, VillageScenic> implements ScenicService {

    private final ScenicMapper scenicMapper;
    private final VillageMapper villageMapper;
    private final FarmerMapper farmerMapper;
    private final UserMapper userMapper;
    private final UserCommentMapper userCommentMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void register(ScenicDTO dto) {
        if (!SecurityUtils.isFarmerOrAbove()) {
            throw new BusinessException("权限不足：仅农户/村长可新增景点");
        }
        Long currentUserId = SecurityUtils.currentUserId();
        FarmerUser fu = farmerMapper.selectOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, currentUserId).last("limit 1"));
        if (fu == null || fu.getVillageId() == null) {
            throw new BusinessException("您还没有所属村落，无法新增景点");
        }
        if (!fu.getVillageId().equals(dto.getVillageId())) {
            throw new BusinessException("操作失败：只能在本村新增景点");
        }
        VillageScenic scenic = BeanUtil.copyProperties(dto, VillageScenic.class);
        scenic.setUserId(currentUserId);
        scenic.setLikes(0);
        scenic.setCollections(0);
        if (scenic.getPrice() == null) {
            scenic.setPrice(0);
        }
        if (scenic.getHasAccommodation() == null) {
            scenic.setHasAccommodation(0);
        }
        if (scenicMapper.insert(scenic) != 1) {
            throw new BusinessException(ErrorConstants.ERROR_INSERT);
        }
    }

    @Override
    public List<ScenicVO> getTop10Scenic() {
        List<VillageScenic> scenics = scenicMapper.selectList(new LambdaQueryWrapper<VillageScenic>()
                .orderByDesc(VillageScenic::getLikes).last("limit 10"));
        List<ScenicVO> voList = scenics.stream().map(s -> BeanUtil.copyProperties(s, ScenicVO.class))
                .collect(Collectors.toList());
        fillScenicVillageNames(voList);
        return voList;
    }

    @Override
    public ScenicVO detail(Long id) {
        VillageScenic scenic = scenicMapper.selectById(id);
        if (scenic == null) {
            throw new BusinessException(ErrorConstants.DATA_NOT_EXIST);
        }
        ScenicVO vo = BeanUtil.copyProperties(scenic, ScenicVO.class);
        fillScenicVillageNames(List.of(vo));
        return vo;
    }

    @Override
    public List<UserCommentVO> getScComments(Long id) {
        String key = RedisConstants.SCENIC_COMMENTS + id;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cached)) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<UserCommentVO>>() {
                });
            } catch (Exception e) {
                log.error("景点评论缓存解析失败，key:{}", key, e);
            }
        }
        List<UserComment> comments = userCommentMapper.selectList(new LambdaQueryWrapper<UserComment>()
                .eq(UserComment::getScenicId, id).orderByDesc(UserComment::getCreateTime));
        List<UserCommentVO> voList = comments.stream().map(c -> BeanUtil.copyProperties(c, UserCommentVO.class))
                .collect(Collectors.toList());
        fillCommentUsernames(voList, comments);
        int ttl = RandomUtil.randomInt(0, 401) + RedisConstants.USER_COMMENTS_TTL;
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(voList), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("景点评论缓存写入失败，key:{}", key, e);
        }
        return voList;
    }

    @Override
    public PageResultVO<ScenicVO> adminList(PageResultDTO pageResultDTO) {
        SecurityUtils.requireAdmin();
        int pageNo = pageResultDTO.getPageNo() == null || pageResultDTO.getPageNo() < 1 ? 1 : pageResultDTO.getPageNo();
        int pageSize = pageResultDTO.getPageSize() == null || pageResultDTO.getPageSize() < 1 ? 10 : pageResultDTO.getPageSize();
        Page<VillageScenic> page = scenicMapper.selectPage(Page.of(pageNo, pageSize),
                new LambdaQueryWrapper<VillageScenic>().orderByDesc(VillageScenic::getCreateTime));
        List<ScenicVO> voList = page.getRecords().stream().map(s -> BeanUtil.copyProperties(s, ScenicVO.class))
                .collect(Collectors.toList());
        fillScenicVillageNames(voList);
        return new PageResultVO<>(page.getTotal(), voList);
    }

    @Override
    public void adminAdd(Long villageId, ScenicDTO dto) {
        SecurityUtils.requireAdmin();
        if (villageMapper.selectById(villageId) == null) {
            throw new BusinessException("该村落不存在");
        }
        VillageScenic scenic = BeanUtil.copyProperties(dto, VillageScenic.class);
        scenic.setVillageId(villageId);
        scenic.setUserId(SecurityUtils.currentUserId());
        scenic.setLikes(0);
        scenic.setCollections(0);
        if (scenic.getPrice() == null) {
            scenic.setPrice(0);
        }
        if (scenic.getHasAccommodation() == null) {
            scenic.setHasAccommodation(0);
        }
        scenicMapper.insert(scenic);
    }

    @Override
    public void adminUpdate(Long id, ScenicDTO dto) {
        SecurityUtils.requireAdmin();
        if (scenicMapper.selectById(id) == null) {
            throw new BusinessException(ErrorConstants.DATA_NOT_EXIST);
        }
        VillageScenic scenic = BeanUtil.copyProperties(dto, VillageScenic.class);
        scenic.setId(id);
        scenicMapper.updateById(scenic);
    }

    @Override
    public void adminDelete(Long id) {
        SecurityUtils.requireAdmin();
        if (scenicMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorConstants.ERROR_DELETE);
        }
    }

    // ===================== 私有工具 =====================

    private void fillScenicVillageNames(List<ScenicVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        Set<Long> villageIds = voList.stream().map(ScenicVO::getVillageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (villageIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = villageMapper.selectBatchIds(villageIds).stream()
                .collect(Collectors.toMap(VillageBase::getId, VillageBase::getName, (a, b) -> a));
        voList.forEach(v -> v.setVillageName(nameMap.get(v.getVillageId())));
    }

    private void fillCommentUsernames(List<UserCommentVO> voList, List<UserComment> comments) {
        if (comments.isEmpty()) {
            return;
        }
        Set<Long> userIds = comments.stream().map(UserComment::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        for (int i = 0; i < voList.size(); i++) {
            voList.get(i).setUsername(nameMap.get(comments.get(i).getUserId()));
        }
    }
}

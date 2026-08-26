package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixiaoyu.xiangyueproject.constants.ErrorConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.VillageBaseDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import com.shixiaoyu.xiangyueproject.utils.SecurityUtil;
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

import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.VILLAGE_COLLECTIONS_TOP_10;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.VILLAGE_LIKES_TOP_10;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.VILLAGE_TOP_10_TTL;

/**
 * 农村信息服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VillageServiceImpl extends ServiceImpl<VillageMapper, VillageBase> implements VillageService {

    private final VillageMapper villageMapper;
    private final ScenicMapper scenicMapper;
    private final FarmerMapper farmerMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PageResultVO<VillageBaseVO> villageList(PageResultDTO pageResultDTO) {
        int pageNo = pageResultDTO.getPageNo() == null || pageResultDTO.getPageNo() < 1 ? 1 : pageResultDTO.getPageNo();
        int pageSize = pageResultDTO.getPageSize() == null || pageResultDTO.getPageSize() < 1 ? 10 : pageResultDTO.getPageSize();
        Page<VillageBase> page = Page.of(pageNo, pageSize);
        page.addOrder(OrderItem.asc("create_time"));
        Page<VillageBase> result = page(page);
        List<VillageBaseVO> voList = result.getRecords().stream()
                .map(po -> BeanUtil.copyProperties(po, VillageBaseVO.class))
                .collect(Collectors.toList());
        fillManagerNames(voList, result.getRecords());
        return new PageResultVO<>(result.getTotal(), voList);
    }

    @Override
    public void addVillage(VillageBaseDTO dto) {
        SecurityUtil.requireAdmin();
        if (dto == null) {
            throw new BusinessException(ErrorConstants.INSERT_NULL);
        }
        VillageBase villageBase = BeanUtil.copyProperties(dto, VillageBase.class);
        if (villageMapper.insert(villageBase) != 1) {
            throw new BusinessException(ErrorConstants.ERROR_INSERT);
        }
    }

    @Override
    public void updateVillage(Long id, VillageBaseDTO dto) {
        SecurityUtil.requireAdmin();
        if (id == null) {
            throw new BusinessException(ErrorConstants.NULL_ID);
        }
        if (getById(id) == null) {
            throw new BusinessException(ErrorConstants.DATA_NOT_EXIST);
        }
        VillageBase villageBase = BeanUtil.copyProperties(dto, VillageBase.class);
        villageBase.setId(id);
        if (!updateById(villageBase)) {
            throw new BusinessException(ErrorConstants.ERROR_UPDATE);
        }
    }

    @Override
    public void deleteVillage(Long id) {
        SecurityUtil.requireAdmin();
        if (id == null) {
            throw new BusinessException(ErrorConstants.NULL_ID);
        }
        if (getById(id) == null) {
            throw new BusinessException(ErrorConstants.DATA_NOT_EXIST);
        }
        if (!removeById(id)) {
            throw new BusinessException(ErrorConstants.ERROR_DELETE);
        }
    }

    @Override
    public List<VillageBaseVO> villageLikes() {
        return getTop10(VILLAGE_LIKES_TOP_10, true);
    }

    @Override
    public List<VillageBaseVO> villageCollections() {
        return getTop10(VILLAGE_COLLECTIONS_TOP_10, false);
    }

    // ===================== 私有工具 =====================

    /** top10 排行：先查缓存，未命中则按下属景点点赞/收藏聚合计算并回填缓存 */
    private List<VillageBaseVO> getTop10(String cacheKey, boolean byLikes) {
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<VillageBaseVO>>() {
                });
            } catch (Exception e) {
                log.error("top10 缓存解析失败，key:{}", cacheKey, e);
            }
        }
        List<VillageBase> villages = villageMapper.selectList(null);
        if (villages.isEmpty()) {
            return List.of();
        }
        // 一次性查出全部景点，按村落聚合点赞/收藏总和，避免 N+1
        List<VillageScenic> allScenics = scenicMapper.selectList(null);
        Map<Long, Integer> totalMap = allScenics.stream().collect(Collectors.groupingBy(
                VillageScenic::getVillageId,
                Collectors.summingInt(s -> byLikes
                        ? (s.getLikes() == null ? 0 : s.getLikes())
                        : (s.getCollections() == null ? 0 : s.getCollections()))));
        List<VillageBase> topVillages = villages.stream()
                .sorted((a, b) -> Integer.compare(totalMap.getOrDefault(b.getId(), 0), totalMap.getOrDefault(a.getId(), 0)))
                .limit(10)
                .collect(Collectors.toList());
        List<VillageBaseVO> voList = topVillages.stream().map(v -> {
            VillageBaseVO vo = BeanUtil.copyProperties(v, VillageBaseVO.class);
            if (byLikes) {
                vo.setLikes(totalMap.get(v.getId()));
            } else {
                vo.setCollects(totalMap.get(v.getId()));
            }
            return vo;
        }).collect(Collectors.toList());
        fillManagerNames(voList, topVillages);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(voList), VILLAGE_TOP_10_TTL, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("top10 缓存写入失败，key:{}", cacheKey, e);
        }
        return voList;
    }

    /** 批量填充村长展示名 */
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

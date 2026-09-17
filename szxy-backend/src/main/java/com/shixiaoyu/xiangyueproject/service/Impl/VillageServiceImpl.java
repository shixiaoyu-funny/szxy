package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.constants.ErrorConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.VillageBaseDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageDetailVO;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.FarmerService;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import com.shixiaoyu.xiangyueproject.service.support.ScenicVoFiller;
import com.shixiaoyu.xiangyueproject.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ScenicVoFiller scenicVoFiller;
    private final FarmerService farmerService;

    /**
     * 管理端分页查询农村列表
     *
     * @param pageResultDTO 分页参数
     */
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

    /**
     * 农村详情：基础信息 + 村长名 + 下属景点列表 + 村内点赞/收藏汇总
     *
     * @param id 农村ID
     */
    @Override
    public VillageDetailVO detail(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorConstants.NULL_ID);
        }
        VillageBase village = villageMapper.selectById(id);
        if (village == null) {
            throw new BusinessException(ErrorConstants.DATA_NOT_EXIST);
        }
        VillageDetailVO vo = BeanUtil.copyProperties(village, VillageDetailVO.class);
        fillManagerNames(List.of(vo), List.of(village));

        List<VillageScenic> scenics = scenicMapper.selectList(new LambdaQueryWrapper<VillageScenic>()
                .eq(VillageScenic::getVillageId, id)
                .orderByDesc(VillageScenic::getLikes));
        List<ScenicVO> scenicVOList = scenics.stream()
                .map(s -> BeanUtil.copyProperties(s, ScenicVO.class))
                .collect(Collectors.toList());
        scenicVOList.forEach(s -> s.setVillageName(village.getName()));
        scenicVoFiller.fillTicketPrice(scenicVOList);
        vo.setScenics(scenicVOList);

        int totalLikes = scenics.stream()
                .mapToInt(s -> s.getLikes() == null ? 0 : s.getLikes())
                .sum();
        int totalCollects = scenics.stream()
                .mapToInt(s -> s.getCollections() == null ? 0 : s.getCollections())
                .sum();
        vo.setLikes(totalLikes);
        vo.setCollects(totalCollects);
        return vo;
    }

    /**
     * 管理端新增农村
     *
     * @param dto 农村信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addVillage(VillageBaseDTO dto) {
        SecurityUtil.requireAdmin();
        if (dto == null) {
            throw new BusinessException(ErrorConstants.INSERT_NULL);
        }
        VillageBase villageBase = BeanUtil.copyProperties(dto, VillageBase.class);
        Long manageId = villageBase.getManageId();
        // 村长任命由 setVillageManager 统一管理（含角色升降），insert 时先不写 manage_id
        villageBase.setManageId(null);
        if (villageMapper.insert(villageBase) != 1) {
            throw new BusinessException(ErrorConstants.ERROR_INSERT);
        }
        if (manageId != null) {
            farmerService.setVillageManager(villageBase.getId(), manageId);
        }
    }

    /**
     * 管理端修改农村
     *
     * @param id  农村ID
     * @param dto 农村信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVillage(Long id, VillageBaseDTO dto) {
        SecurityUtil.requireAdmin();
        if (id == null) {
            throw new BusinessException(ErrorConstants.NULL_ID);
        }
        if (getById(id) == null) {
            throw new BusinessException(ErrorConstants.DATA_NOT_EXIST);
        }
        VillageBase villageBase = BeanUtil.copyProperties(dto, VillageBase.class);
        Long manageId = villageBase.getManageId();
        // manage_id 交由任命逻辑维护，避免 updateById 覆盖
        villageBase.setManageId(null);
        villageBase.setId(id);
        if (!updateById(villageBase)) {
            throw new BusinessException(ErrorConstants.ERROR_UPDATE);
        }
        Long currentManageId = getById(id).getManageId();
        if (manageId != null && !manageId.equals(currentManageId)) {
            farmerService.setVillageManager(id, manageId);
        }
    }

    /**
     * 管理端删除农村
     *
     * @param id 农村ID
     */
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

    /**
     * 优质农村 TOP10（按下属景点总点赞量）
     */
    @Override
    public List<VillageBaseVO> villageLikes() {
        return getTop10(VILLAGE_LIKES_TOP_10, true);
    }

    /**
     * 优质农村 TOP10（按下属景点总收藏量）
     */
    @Override
    public List<VillageBaseVO> villageCollections() {
        return getTop10(VILLAGE_COLLECTIONS_TOP_10, false);
    }

    // ===================== 私有工具 =====================

    /**
     * TOP10 排行：先查 Redis 缓存，未命中则聚合计算后回填
     *
     * @param cacheKey Redis 键（点赞榜 / 收藏榜）
     * @param byLikes  true=按点赞汇总，false=按收藏汇总
     */
    private List<VillageBaseVO> getTop10(String cacheKey, boolean byLikes) {
        // 1. 读缓存：命中则 JSON 反序列化直接返回
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            try {
                // JSONUtil：把 Redis 里的 JSON 字符串还原成 List<VillageBaseVO>
                return JSONUtil.toList(cached, VillageBaseVO.class);
            } catch (Exception e) {
                log.error("top10 缓存解析失败，key:{}", cacheKey, e);
            }
        }
        // 2. 缓存未命中：查全部村 + 全部景点，按村聚合点赞/收藏总和
        List<VillageBase> villages = villageMapper.selectList(null);
        if (villages.isEmpty()) {
            return List.of();
        }
        List<VillageScenic> allScenics = scenicMapper.selectList(null);
        // groupingBy：按 villageId 分组；summingInt：组内求和（避免逐村查景点 N+1）
        Map<Long, Integer> totalMap = allScenics.stream().collect(Collectors.groupingBy(
                VillageScenic::getVillageId,
                Collectors.summingInt(s -> byLikes
                        ? (s.getLikes() == null ? 0 : s.getLikes())
                        : (s.getCollections() == null ? 0 : s.getCollections()))));
        // 3. 排序取前 10，转 VO 并填充村长名
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
        // 4. 写回 Redis：List 序列化成 JSON，设置 TTL
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(voList), VILLAGE_TOP_10_TTL, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("top10 缓存写入失败，key:{}", cacheKey, e);
        }
        return voList;
    }

    /**
     * 批量填充村长展示名（一次 IN 查询，避免每条村单独查 user）
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
}

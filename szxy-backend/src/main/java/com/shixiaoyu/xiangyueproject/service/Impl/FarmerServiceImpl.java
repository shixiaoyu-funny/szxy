package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerUserDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerUserVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.FarmerService;
import com.shixiaoyu.xiangyueproject.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 农户服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerServiceImpl extends ServiceImpl<FarmerMapper, FarmerUser> implements FarmerService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final FarmerMapper farmerMapper;
    private final UserMapper userMapper;
    private final VillageMapper villageMapper;
    private final ScenicMapper scenicMapper;
    private final BCryptPasswordEncoder encoder;

    @Override
    public List<FarmerUserVO> getFarmersByVillage() {
        Long currentUserId = SecurityUtils.currentUserId();
        Long villageId = getChiefVillageId(currentUserId);
        if (villageId == null) {
            throw new BusinessException("权限不足：您不是任何村落的管理员（村长）");
        }
        return farmerMapper.selectFarmersByVillage(villageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFarmer(Long villageId, FarmerUserDTO dto) {
        Long currentUserId = SecurityUtils.currentUserId();
        Long myVillage = getChiefVillageId(currentUserId);
        if (myVillage == null) {
            throw new BusinessException("权限不足：您不是任何村落的管理员（村长）");
        }
        if (!myVillage.equals(villageId)) {
            throw new BusinessException("操作失败：只能在本村新增农户");
        }
        createFarmerInternal(villageId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFarmer(Long farmerUserId, FarmerUserDTO dto) {
        Long currentUserId = SecurityUtils.currentUserId();
        Long myVillage = getChiefVillageId(currentUserId);
        if (myVillage == null) {
            throw new BusinessException("权限不足：您不是任何村落的管理员（村长）");
        }
        FarmerUser target = getFarmerByUserId(farmerUserId);
        if (target == null) {
            throw new BusinessException("该农户档案不存在");
        }
        if (!myVillage.equals(target.getVillageId())) {
            throw new BusinessException("操作失败：该农户不属于您的管辖范围");
        }
        if (StrUtil.isNotBlank(dto.getPhone())) {
            User exist = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
            if (exist != null && !exist.getId().equals(farmerUserId)) {
                throw new BusinessException("手机号已被占用");
            }
        }
        User user = new User();
        user.setId(farmerUserId);
        user.setUsername(StrUtil.isBlank(dto.getUsername()) ? null : dto.getUsername());
        user.setPhone(dto.getPhone());
        userMapper.updateById(user);

        FarmerUser fu = new FarmerUser();
        fu.setId(target.getId());
        fu.setIdCard(dto.getIdCard());
        fu.setBusinessType(dto.getBusinessType());
        farmerMapper.updateById(fu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFarmer(Long farmerUserId) {
        Long currentUserId = SecurityUtils.currentUserId();
        Long myVillage = getChiefVillageId(currentUserId);
        if (myVillage == null) {
            throw new BusinessException("权限不足：您不是任何村落的管理员（村长）");
        }
        FarmerUser target = getFarmerByUserId(farmerUserId);
        if (target == null) {
            throw new BusinessException("该农户档案不存在");
        }
        if (!myVillage.equals(target.getVillageId())) {
            throw new BusinessException("操作失败：该农户不属于您的管辖范围");
        }
        farmerMapper.deleteById(target.getId());
        User user = new User();
        user.setId(farmerUserId);
        user.setRole(RoleEnum.VISITOR);
        userMapper.updateById(user);
    }

    @Override
    public List<FarmerUserVO> getAllFarmers() {
        return farmerMapper.selectAllFarmers();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFarmer(Long villageId, FarmerUserDTO dto) {
        if (villageMapper.selectById(villageId) == null) {
            throw new BusinessException("该村落不存在");
        }
        createFarmerInternal(villageId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setVillageManager(Long villageId, Long farmerUserId) {
        VillageBase village = villageMapper.selectById(villageId);
        if (village == null) {
            throw new BusinessException("该村落不存在");
        }
        User newChief = userMapper.selectById(farmerUserId);
        if (newChief == null) {
            throw new BusinessException("该用户不存在");
        }
        if (getFarmerByUserId(farmerUserId) == null) {
            throw new BusinessException("该用户还不是农户，请先在村内建档");
        }
        Long oldChiefId = village.getManageId();
        if (oldChiefId != null && !oldChiefId.equals(farmerUserId)) {
            User oldChief = new User();
            oldChief.setId(oldChiefId);
            oldChief.setRole(RoleEnum.FARMER);
            userMapper.updateById(oldChief);
            log.info("村落 {} 旧村长 {} 已降为农户", villageId, oldChiefId);
        }
        User promote = new User();
        promote.setId(farmerUserId);
        promote.setRole(RoleEnum.CHIEF);
        userMapper.updateById(promote);
        village.setManageId(farmerUserId);
        villageMapper.updateById(village);
        log.info("村落 {} 任命 {} 为村长", villageId, farmerUserId);
    }

    @Override
    public VillageBaseVO getMyVillage() {
        Long currentUserId = SecurityUtils.currentUserId();
        FarmerUser fu = getFarmerByUserId(currentUserId);
        if (fu == null || fu.getVillageId() == null) {
            throw new BusinessException("您还没有所属村落");
        }
        VillageBase villageBase = villageMapper.selectById(fu.getVillageId());
        if (villageBase == null) {
            throw new BusinessException("所属村落不存在");
        }
        VillageBaseVO vo = BeanUtil.copyProperties(villageBase, VillageBaseVO.class);
        fillManagerName(vo, villageBase);
        return vo;
    }

    @Override
    public List<ScenicVO> getMyScenics() {
        Long currentUserId = SecurityUtils.currentUserId();
        List<VillageScenic> scenics = scenicMapper.selectList(
                new LambdaQueryWrapper<VillageScenic>().eq(VillageScenic::getUserId, currentUserId)
                        .orderByDesc(VillageScenic::getCreateTime));
        List<ScenicVO> voList = scenics.stream().map(s -> BeanUtil.copyProperties(s, ScenicVO.class))
                .collect(Collectors.toList());
        fillScenicVillageNames(voList);
        return voList;
    }

    // ===================== 私有工具 =====================

    /** 建档农户：创建 user 账号（默认密码）+ farm_user 档案 */
    private void createFarmerInternal(Long villageId, FarmerUserDTO dto) {
        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new BusinessException("手机号不能为空");
        }
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (exist != null) {
            throw new BusinessException("手机号 " + dto.getPhone() + " 已被占用");
        }
        String username = StrUtil.isNotBlank(dto.getUsername())
                ? dto.getUsername()
                : dto.getPhone();
        User user = new User();
        user.setUsername(username);
        user.setPhone(dto.getPhone());
        user.setPassword(encoder.encode(DEFAULT_PASSWORD));
        user.setRole(RoleEnum.FARMER);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("建档农户账号成功，userId:{}，初始密码:{}", user.getId(), DEFAULT_PASSWORD);

        FarmerUser fu = new FarmerUser();
        fu.setUserId(user.getId());
        fu.setVillageId(villageId);
        fu.setIdCard(dto.getIdCard());
        fu.setBusinessType(dto.getBusinessType());
        farmerMapper.insert(fu);
    }

    /** 当前村长管辖的村落 ID（通过 village_base.manage_id 判定） */
    private Long getChiefVillageId(Long userId) {
        VillageBase village = villageMapper.selectOne(new LambdaQueryWrapper<VillageBase>()
                .eq(VillageBase::getManageId, userId).last("limit 1"));
        return village == null ? null : village.getId();
    }

    private FarmerUser getFarmerByUserId(Long userId) {
        return farmerMapper.selectOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, userId).last("limit 1"));
    }

    private void fillManagerName(VillageBaseVO vo, VillageBase villageBase) {
        Long manageId = villageBase.getManageId();
        if (manageId == null) {
            return;
        }
        User u = userMapper.selectById(manageId);
        if (u != null) {
            vo.setManagerName(u.getUsername());
        }
    }

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
}

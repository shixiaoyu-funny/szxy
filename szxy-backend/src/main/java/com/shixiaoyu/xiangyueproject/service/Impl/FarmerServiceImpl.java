package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
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
import com.shixiaoyu.xiangyueproject.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 本村农户列表：校验村长身份后按村查询并组装 VO
     */
    @Override
    public List<FarmerUserVO> getFarmersByVillage() {
        Long villageId = requireChiefVillageId(SecurityUtil.currentUserId());
        return toFarmerVos(farmerMapper.selectList(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getVillageId, villageId)
                .orderByDesc(FarmerUser::getId)));
    }

    /**
     * 村长新增本村农户：校验本村权限后建档
     * @param villageId 村落ID
     * @param dto 农户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFarmer(Long villageId, FarmerUserDTO dto) {
        Long myVillage = requireChiefVillageId(SecurityUtil.currentUserId());
        assertSameVillage(myVillage, villageId, "操作失败：只能在本村新增农户");
        createFarmerInternal(villageId, dto);
    }

    /**
     * 村长修改本村农户：校验管辖范围后更新 user + farm_user
     * @param farmerUserId 农户 user.id
     * @param dto 农户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFarmer(Long farmerUserId, FarmerUserDTO dto) {
        Long myVillage = requireChiefVillageId(SecurityUtil.currentUserId());
        FarmerUser target = requireManagedFarmer(farmerUserId, myVillage);
        assertPhoneAvailable(dto.getPhone(), farmerUserId);
        updateFarmerUserAccount(farmerUserId, dto);
        updateFarmerProfile(target.getId(), dto);
    }

    /**
     * 村长删除本村农户：删档案并将账号降为游客
     * @param farmerUserId 农户 user.id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFarmer(Long farmerUserId) {
        Long myVillage = requireChiefVillageId(SecurityUtil.currentUserId());
        FarmerUser target = requireManagedFarmer(farmerUserId, myVillage);
        farmerMapper.deleteById(target.getId());
        demoteToVisitor(farmerUserId);
    }

    /**
     * 管理端：全量农户列表
     */
    @Override
    public List<FarmerUserVO> getAllFarmers() {
        SecurityUtil.requireAdmin();
        return toFarmerVos(farmerMapper.selectList(new LambdaQueryWrapper<FarmerUser>()
                .orderByDesc(FarmerUser::getId)));
    }

    /**
     * 管理端建档农户：校验村落后创建账号+档案
     * @param villageId 村落ID
     * @param dto 农户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFarmer(Long villageId, FarmerUserDTO dto) {
        SecurityUtil.requireAdmin();
        requireVillageExists(villageId);
        createFarmerInternal(villageId, dto);
    }

    /**
     * 管理端任命村长：事务内同步 manage_id 与角色升降
     * @param villageId 村落ID
     * @param farmerUserId 目标农户 user.id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setVillageManager(Long villageId, Long farmerUserId) {
        SecurityUtil.requireAdmin();
        VillageBase village = requireVillage(villageId);
        requireUserExists(farmerUserId);
        FarmerUser target = requireFarmerInVillage(farmerUserId, villageId);
        demoteOldChiefIfNeeded(village, farmerUserId);
        promoteToChief(farmerUserId);
        village.setManageId(farmerUserId);
        villageMapper.updateById(village);
        log.info("村落 {} 任命 {} 为村长（档案id={}）", villageId, farmerUserId, target.getId());
    }

    /**
     * 我的所属村落信息
     */
    @Override
    public VillageBaseVO getMyVillage() {
        FarmerUser fu = getFarmerByUserId(SecurityUtil.currentUserId());
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

    /**
     * 我创建的景点列表
     */
    @Override
    public List<ScenicVO> getMyScenics() {
        Long currentUserId = SecurityUtil.currentUserId();
        List<VillageScenic> scenics = scenicMapper.selectList(
                new LambdaQueryWrapper<VillageScenic>().eq(VillageScenic::getUserId, currentUserId)
                        .orderByDesc(VillageScenic::getCreateTime));
        List<ScenicVO> voList = scenics.stream().map(s -> BeanUtil.copyProperties(s, ScenicVO.class))
                .collect(Collectors.toList());
        fillScenicVillageNames(voList);
        return voList;
    }

    // ===================== 二级方法 =====================

    // 校验当前用户为村长并返回管辖村 ID
    private Long requireChiefVillageId(Long userId) {
        Long villageId = getChiefVillageId(userId);
        if (villageId == null) {
            throw new BusinessException("权限不足：您不是任何村落的管理员（村长）");
        }
        return villageId;
    }

    // 校验村落存在
    private void requireVillageExists(Long villageId) {
        if (villageMapper.selectById(villageId) == null) {
            throw new BusinessException("该村落不存在");
        }
    }

    // 取村落实体
    private VillageBase requireVillage(Long villageId) {
        VillageBase village = villageMapper.selectById(villageId);
        if (village == null) {
            throw new BusinessException("该村落不存在");
        }
        return village;
    }

    // 校验用户存在
    private void requireUserExists(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException("该用户不存在");
        }
    }

    // 两村必须一致
    private void assertSameVillage(Long expected, Long actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new BusinessException(message);
        }
    }

    // 本村管辖内的农户档案
    private FarmerUser requireManagedFarmer(Long farmerUserId, Long myVillage) {
        FarmerUser target = getFarmerByUserId(farmerUserId);
        if (target == null) {
            throw new BusinessException("该农户档案不存在");
        }
        assertSameVillage(myVillage, target.getVillageId(), "操作失败：该农户不属于您的管辖范围");
        return target;
    }

    // 指定村内的农户档案
    private FarmerUser requireFarmerInVillage(Long farmerUserId, Long villageId) {
        FarmerUser target = getFarmerByUserId(farmerUserId);
        if (target == null) {
            throw new BusinessException("该用户还不是农户，请先在村内建档");
        }
        assertSameVillage(villageId, target.getVillageId(), "操作失败：该农户不属于目标村落");
        return target;
    }

    // 手机号未被他人占用
    private void assertPhoneAvailable(String phone, Long selfUserId) {
        if (StrUtil.isBlank(phone)) {
            return;
        }
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (exist != null && !exist.getId().equals(selfUserId)) {
            throw new BusinessException("手机号已被占用");
        }
    }

    // 更新农户登录账号字段
    private void updateFarmerUserAccount(Long farmerUserId, FarmerUserDTO dto) {
        User user = new User();
        user.setId(farmerUserId);
        user.setUsername(StrUtil.isBlank(dto.getUsername()) ? null : dto.getUsername());
        user.setPhone(dto.getPhone());
        userMapper.updateById(user);
    }

    // 更新农户档案字段
    private void updateFarmerProfile(Long farmUserId, FarmerUserDTO dto) {
        FarmerUser fu = new FarmerUser();
        fu.setId(farmUserId);
        fu.setIdCard(dto.getIdCard());
        fu.setBusinessType(dto.getBusinessType());
        farmerMapper.updateById(fu);
    }

    // 账号降为游客
    private void demoteToVisitor(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(RoleEnum.VISITOR);
        userMapper.updateById(user);
    }

    // 旧村长降为农户
    private void demoteOldChiefIfNeeded(VillageBase village, Long newChiefId) {
        Long oldChiefId = village.getManageId();
        if (oldChiefId == null || oldChiefId.equals(newChiefId)) {
            return;
        }
        User oldChief = new User();
        oldChief.setId(oldChiefId);
        oldChief.setRole(RoleEnum.FARMER);
        userMapper.updateById(oldChief);
        log.info("村落 {} 旧村长 {} 已降为农户", village.getId(), oldChiefId);
    }

    // 升为村长角色
    private void promoteToChief(Long farmerUserId) {
        User promote = new User();
        promote.setId(farmerUserId);
        promote.setRole(RoleEnum.CHIEF);
        userMapper.updateById(promote);
    }

    // farm_user 列表 + 批量查 user / village，组装 VO
    private List<FarmerUserVO> toFarmerVos(List<FarmerUser> farmers) {
        if (farmers == null || farmers.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = farmers.stream()
                .map(FarmerUser::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 批量填充村落名称
        Set<Long> villageIds = farmers.stream()
                .map(FarmerUser::getVillageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> villageNameMap = villageIds.isEmpty()
                ? Map.of()
                : villageMapper.selectByIds(villageIds).stream()
                .collect(Collectors.toMap(VillageBase::getId, VillageBase::getName, (a, b) -> a));

        return farmers.stream().map(fu -> {
            FarmerUserVO vo = BeanUtil.copyProperties(fu, FarmerUserVO.class);
            User u = userMap.get(fu.getUserId());
            if (u != null) {
                vo.setUsername(u.getUsername());
                vo.setPhone(u.getPhone());
                vo.setRole(u.getRole());
            }
            vo.setVillageName(villageNameMap.get(fu.getVillageId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 建档农户：创建 user 账号（默认密码）+ farm_user 档案
     * @param villageId 村落ID
     * @param dto 农户信息
     */
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
        user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
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

    // 当前村长管辖的村落 ID
    private Long getChiefVillageId(Long userId) {
        VillageBase village = villageMapper.selectOne(new LambdaQueryWrapper<VillageBase>()
                .eq(VillageBase::getManageId, userId).last("limit 1"));
        return village == null ? null : village.getId();
    }

    // 按 userId 查农户档案
    private FarmerUser getFarmerByUserId(Long userId) {
        return farmerMapper.selectOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, userId).last("limit 1"));
    }

    // 填充村长姓名
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

    // 批量填充景点所属村落名
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
}

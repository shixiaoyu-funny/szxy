package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.VgHeadAccess;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VgHeadAccessVO;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import com.shixiaoyu.xiangyueproject.enums.VgHeadStatusEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VgHeadAccessMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.VgHeadAccessService;
import com.shixiaoyu.xiangyueproject.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 村长准入申请服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VgHeadAccessServiceImpl extends ServiceImpl<VgHeadAccessMapper, VgHeadAccess>
        implements VgHeadAccessService {

    private final VgHeadAccessMapper vgHeadAccessMapper;
    private final FarmerMapper farmerMapper;
    private final UserMapper userMapper;
    private final VillageMapper villageMapper;

    /**
     * 农户申请成为本村村长：校验身份后写入待审单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply() {
        Long userId = SecurityUtil.currentUserId();
        FarmerUser fu = requireFarmerProfile(userId);
        assertNotAlreadyChief(userId, fu.getVillageId());
        assertNoActiveApply(fu.getId());

        VgHeadAccess access = new VgHeadAccess();
        access.setVillageId(fu.getVillageId());
        access.setFuId(fu.getId());
        access.setStatus(VgHeadStatusEnum.PENDING);
        vgHeadAccessMapper.insert(access);
        log.info("村长申请已提交，fuId={}, villageId={}", fu.getId(), fu.getVillageId());
    }

    /**
     * 本人最新一条申请
     */
    @Override
    public VgHeadAccessVO mine() {
        FarmerUser fu = farmerMapper.selectOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, SecurityUtil.currentUserId())
                .last("limit 1"));
        if (fu == null) {
            return null;
        }
        VgHeadAccess latest = vgHeadAccessMapper.selectOne(new LambdaQueryWrapper<VgHeadAccess>()
                .eq(VgHeadAccess::getFuId, fu.getId())
                .orderByDesc(VgHeadAccess::getId)
                .last("limit 1"));
        return latest == null ? null : toVo(latest);
    }

    /**
     * 本人全部申请（消息页申请列表）
     */
    @Override
    public List<VgHeadAccessVO> myList() {
        FarmerUser fu = farmerMapper.selectOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, SecurityUtil.currentUserId())
                .last("limit 1"));
        if (fu == null) {
            return Collections.emptyList();
        }
        List<VgHeadAccess> list = vgHeadAccessMapper.selectList(new LambdaQueryWrapper<VgHeadAccess>()
                .eq(VgHeadAccess::getFuId, fu.getId())
                .orderByDesc(VgHeadAccess::getUpdateTime));
        return toVoList(list);
    }

    /**
     * 本村村长：待审列表
     */
    @Override
    public List<VgHeadAccessVO> pendingForChief() {
        Long villageId = requireChiefVillageId(SecurityUtil.currentUserId());
        List<VgHeadAccess> list = vgHeadAccessMapper.selectList(new LambdaQueryWrapper<VgHeadAccess>()
                .eq(VgHeadAccess::getVillageId, villageId)
                .eq(VgHeadAccess::getStatus, VgHeadStatusEnum.PENDING)
                .orderByDesc(VgHeadAccess::getCreateTime));
        return toVoList(list);
    }

    /**
     * 申请详情（本人 / 本村村长 / 管理员）
     * @param id 申请ID
     */
    @Override
    public VgHeadAccessVO detail(Long id) {
        VgHeadAccess access = requireAccess(id);
        assertCanView(access);
        return toVo(access);
    }

    /**
     * 村长通过：0→1（村必须已有村长）
     * @param id 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void chiefApprove(Long id) {
        Long villageId = requireChiefVillageId(SecurityUtil.currentUserId());
        VgHeadAccess access = requireAccess(id);
        assertSameVillage(villageId, access.getVillageId());
        if (access.getStatus() != VgHeadStatusEnum.PENDING) {
            throw new BusinessException("该申请当前不可由村长审批");
        }
        VillageBase village = villageMapper.selectById(villageId);
        if (village == null || village.getManageId() == null) {
            throw new BusinessException("当前村落无村长，请等待管理员直接审批");
        }
        updateStatus(access.getId(), VgHeadStatusEnum.CHIEF_APPROVED);
    }

    /**
     * 村长拒绝：0→3
     * @param id 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void chiefReject(Long id) {
        Long villageId = requireChiefVillageId(SecurityUtil.currentUserId());
        VgHeadAccess access = requireAccess(id);
        assertSameVillage(villageId, access.getVillageId());
        if (access.getStatus() != VgHeadStatusEnum.PENDING) {
            throw new BusinessException("该申请当前不可由村长审批");
        }
        updateStatus(access.getId(), VgHeadStatusEnum.REJECTED);
    }

    /**
     * 管理端分页列表
     */
    @Override
    public PageResultVO<VgHeadAccessVO> adminList(PageResultDTO pageResultDTO, VgHeadStatusEnum status) {
        SecurityUtil.requireAdmin();
        int pageNo = pageResultDTO == null || pageResultDTO.getPageNo() == null || pageResultDTO.getPageNo() < 1
                ? 1 : pageResultDTO.getPageNo();
        int pageSize = pageResultDTO == null || pageResultDTO.getPageSize() == null || pageResultDTO.getPageSize() < 1
                ? 10 : pageResultDTO.getPageSize();

        LambdaQueryWrapper<VgHeadAccess> wrapper = new LambdaQueryWrapper<VgHeadAccess>()
                .eq(status != null, VgHeadAccess::getStatus, status)
                .orderByDesc(VgHeadAccess::getUpdateTime);
        Page<VgHeadAccess> page = vgHeadAccessMapper.selectPage(Page.of(pageNo, pageSize), wrapper);
        return new PageResultVO<>(page.getTotal(), toVoList(page.getRecords()));
    }

    /**
     * 管理员终审通过：换村长 + status=2，并拒绝同村其他进行中申请
     * @param id 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminApprove(Long id) {
        SecurityUtil.requireAdmin();
        VgHeadAccess access = requireAccess(id);
        VillageBase village = requireVillage(access.getVillageId());
        assertAdminCanApprove(access, village);

        FarmerUser fu = farmerMapper.selectById(access.getFuId());
        if (fu == null || fu.getUserId() == null) {
            throw new BusinessException("申请人农户档案不存在");
        }
        transferVillageHead(village, fu.getUserId());
        updateStatus(access.getId(), VgHeadStatusEnum.ADMIN_APPROVED);
        rejectOtherActiveApplies(access.getVillageId(), access.getId());
        log.info("村长申请终审通过，accessId={}, newChiefUserId={}", id, fu.getUserId());
    }

    /**
     * 管理员拒绝
     * @param id 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminReject(Long id) {
        SecurityUtil.requireAdmin();
        VgHeadAccess access = requireAccess(id);
        if (access.getStatus() != VgHeadStatusEnum.PENDING
                && access.getStatus() != VgHeadStatusEnum.CHIEF_APPROVED) {
            throw new BusinessException("该申请当前不可拒绝");
        }
        VillageBase village = requireVillage(access.getVillageId());
        // 有村长时，管理员通常审 status=1；无村长时审 status=0
        if (village.getManageId() != null && access.getStatus() == VgHeadStatusEnum.PENDING) {
            throw new BusinessException("该村已有村长，请等待村长审批后再由管理员处理");
        }
        updateStatus(access.getId(), VgHeadStatusEnum.REJECTED);
    }

    /**
     * 本村待审数量（消息角标）
     */
    @Override
    public int pendingChiefCount() {
        Long villageId = getChiefVillageId(SecurityUtil.currentUserId());
        if (villageId == null) {
            return 0;
        }
        Long cnt = vgHeadAccessMapper.selectCount(new LambdaQueryWrapper<VgHeadAccess>()
                .eq(VgHeadAccess::getVillageId, villageId)
                .eq(VgHeadAccess::getStatus, VgHeadStatusEnum.PENDING));
        return cnt == null ? 0 : cnt.intValue();
    }

    // ===================== 二级方法 =====================

    // 要求当前用户为农户且有所属村
    private FarmerUser requireFarmerProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() != RoleEnum.FARMER) {
            throw new BusinessException("仅农户可申请成为村长");
        }
        FarmerUser fu = farmerMapper.selectOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, userId).last("limit 1"));
        if (fu == null || fu.getVillageId() == null) {
            throw new BusinessException("您还没有所属村落，无法申请");
        }
        return fu;
    }

    // 已是本村村长不可申请
    private void assertNotAlreadyChief(Long userId, Long villageId) {
        VillageBase village = villageMapper.selectById(villageId);
        if (village != null && userId.equals(village.getManageId())) {
            throw new BusinessException("您已是本村村长");
        }
        if (userMapper.selectById(userId).getRole() == RoleEnum.CHIEF) {
            throw new BusinessException("您当前已是村长角色");
        }
    }

    // 同档案无进行中申请
    private void assertNoActiveApply(Long fuId) {
        Long cnt = vgHeadAccessMapper.selectCount(new LambdaQueryWrapper<VgHeadAccess>()
                .eq(VgHeadAccess::getFuId, fuId)
                .in(VgHeadAccess::getStatus, VgHeadStatusEnum.PENDING, VgHeadStatusEnum.CHIEF_APPROVED));
        if (cnt != null && cnt > 0) {
            throw new BusinessException("您已有进行中的村长申请");
        }
    }

    private VgHeadAccess requireAccess(Long id) {
        VgHeadAccess access = vgHeadAccessMapper.selectById(id);
        if (access == null) {
            throw new BusinessException("申请记录不存在");
        }
        return access;
    }

    private VillageBase requireVillage(Long villageId) {
        VillageBase village = villageMapper.selectById(villageId);
        if (village == null) {
            throw new BusinessException("村落不存在");
        }
        return village;
    }

    private Long requireChiefVillageId(Long userId) {
        Long villageId = getChiefVillageId(userId);
        if (villageId == null) {
            throw new BusinessException("权限不足：您不是村长");
        }
        return villageId;
    }

    private Long getChiefVillageId(Long userId) {
        VillageBase village = villageMapper.selectOne(new LambdaQueryWrapper<VillageBase>()
                .eq(VillageBase::getManageId, userId).last("limit 1"));
        return village == null ? null : village.getId();
    }

    private void assertSameVillage(Long expected, Long actual) {
        if (!Objects.equals(expected, actual)) {
            throw new BusinessException("只能审批本村申请");
        }
    }

    // 本人 / 本村村长 / 管理员可查看
    private void assertCanView(VgHeadAccess access) {
        if (SecurityUtil.isAdmin()) {
            return;
        }
        Long userId = SecurityUtil.currentUserId();
        Long chiefVillage = getChiefVillageId(userId);
        if (Objects.equals(chiefVillage, access.getVillageId())) {
            return;
        }
        FarmerUser fu = farmerMapper.selectById(access.getFuId());
        if (fu != null && userId.equals(fu.getUserId())) {
            return;
        }
        throw new BusinessException("无权查看该申请");
    }

    // 管理员可审：无村长且0，或有村长且1
    private void assertAdminCanApprove(VgHeadAccess access, VillageBase village) {
        boolean hasChief = village.getManageId() != null;
        if (!hasChief) {
            if (access.getStatus() != VgHeadStatusEnum.PENDING) {
                throw new BusinessException("无村长村落仅可审批待审申请");
            }
            return;
        }
        if (access.getStatus() != VgHeadStatusEnum.CHIEF_APPROVED) {
            throw new BusinessException("该村已有村长，需村长先审批通过后再终审");
        }
    }

    // 换村长：旧村长降农户，新用户升村长，写 manage_id
    private void transferVillageHead(VillageBase village, Long newChiefUserId) {
        Long oldChiefId = village.getManageId();
        // 1. 旧村长存在且不是同一人 → 降为农户
        if (oldChiefId != null && !oldChiefId.equals(newChiefUserId)) {
            User old = new User();
            old.setId(oldChiefId);
            old.setRole(RoleEnum.FARMER);
            userMapper.updateById(old);
        }
        // 2. 新村长升 role=3
        User promote = new User();
        promote.setId(newChiefUserId);
        promote.setRole(RoleEnum.CHIEF);
        userMapper.updateById(promote);
        // 3. 村落 manage_id 指向新村长
        village.setManageId(newChiefUserId);
        villageMapper.updateById(village);
    }

    // 拒绝同村其他进行中申请
    private void rejectOtherActiveApplies(Long villageId, Long keepId) {
        List<VgHeadAccess> others = vgHeadAccessMapper.selectList(new LambdaQueryWrapper<VgHeadAccess>()
                .eq(VgHeadAccess::getVillageId, villageId)
                .ne(VgHeadAccess::getId, keepId)
                .in(VgHeadAccess::getStatus, VgHeadStatusEnum.PENDING, VgHeadStatusEnum.CHIEF_APPROVED));
        for (VgHeadAccess o : others) {
            updateStatus(o.getId(), VgHeadStatusEnum.REJECTED);
        }
    }

    private void updateStatus(Long id, VgHeadStatusEnum status) {
        VgHeadAccess update = new VgHeadAccess();
        update.setId(id);
        update.setStatus(status);
        vgHeadAccessMapper.updateById(update);
    }

    /**
     * 批量转 VO：关联农户档案、用户账号、村落名称（避免 N+1）
     */
    private List<VgHeadAccessVO> toVoList(List<VgHeadAccess> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 1. 收集本页所有农户档案 ID、村落 ID
        Set<Long> fuIds = list.stream().map(VgHeadAccess::getFuId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> villageIds = list.stream().map(VgHeadAccess::getVillageId).filter(Objects::nonNull).collect(Collectors.toSet());

        // 2. 批量查农户档案 → fuId → FarmerUser
        Map<Long, FarmerUser> fuMap = fuIds.isEmpty() ? Map.of()
                : farmerMapper.selectByIds(fuIds).stream()
                .collect(Collectors.toMap(FarmerUser::getId, f -> f, (a, b) -> a));
        // 3. 从档案里再收集 userId，批量查用户账号
        Set<Long> userIds = fuMap.values().stream().map(FarmerUser::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        // 4. 批量查村落名称
        Map<Long, String> villageNameMap = villageIds.isEmpty() ? Map.of()
                : villageMapper.selectByIds(villageIds).stream()
                .collect(Collectors.toMap(VillageBase::getId, VillageBase::getName, (a, b) -> a));

        // 5. 逐条组装 VO：基础字段 + 村名 + 申请人信息
        return list.stream().map(a -> {
            VgHeadAccessVO vo = BeanUtil.copyProperties(a, VgHeadAccessVO.class);
            vo.setVillageName(villageNameMap.get(a.getVillageId()));
            FarmerUser fu = fuMap.get(a.getFuId());
            if (fu != null) {
                vo.setUserId(fu.getUserId());
                User u = userMap.get(fu.getUserId());
                if (u != null) {
                    vo.setUsername(u.getUsername());
                    vo.setPhone(u.getPhone());
                    vo.setAvatar(u.getAvatar());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /** 单条转 VO */
    private VgHeadAccessVO toVo(VgHeadAccess access) {
        return toVoList(List.of(access)).get(0);
    }
}

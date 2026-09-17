package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerAccess;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.UserCacheInfo;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerAccessVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.enums.AccessStatusEnum;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.FarmerAccessMapper;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.FarmerAccessService;
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
 * 农户准入申请服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerAccessServiceImpl extends ServiceImpl<FarmerAccessMapper, FarmerAccess>
        implements FarmerAccessService {

    private final FarmerAccessMapper farmerAccessMapper;
    private final UserMapper userMapper;
    private final VillageMapper villageMapper;
    private final FarmerMapper farmerMapper;

    /**
     * 游客提交/改提申请：校验游客身份 → 组装 info 快照 → 有待审则更新否则新增
     * @param dto 身份证、村落、经营类型
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(FarmerAccessDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        User user = requireVisitorUser(userId);
        validateApplyPayload(dto);
        requireVillageExists(dto.getVillageId());
        assertNotAlreadyApproved(userId);

        UserCacheInfo cacheInfo = buildUserCacheInfo(user);
        FarmerAccess pending = findLatestPending(userId);
        if (pending != null) {
            updatePendingApply(pending, dto, cacheInfo);
            return;
        }
        insertPendingApply(userId, dto, cacheInfo);
    }

    /**
     * 查询本人最新一条申请（入口按钮与表单回填）
     */
    @Override
    public FarmerAccessVO mine() {
        Long userId = SecurityUtil.currentUserId();
        FarmerAccess latest = farmerAccessMapper.selectOne(new LambdaQueryWrapper<FarmerAccess>()
                .eq(FarmerAccess::getUserId, userId)
                .orderByDesc(FarmerAccess::getId)
                .last("limit 1"));
        if (latest == null) {
            return null;
        }
        latest.unpackInfo();
        return toVo(latest, resolveVillageName(latest.getVillageId()));
    }

    /**
     * 管理端分页查询申请列表
     * @param pageResultDTO 分页参数
     * @param status 可选状态筛选
     */
    @Override
    public PageResultVO<FarmerAccessVO> list(PageResultDTO pageResultDTO, AccessStatusEnum status) {
        SecurityUtil.requireAdmin();
        int pageNo = normalizePageNo(pageResultDTO);
        int pageSize = normalizePageSize(pageResultDTO);

        LambdaQueryWrapper<FarmerAccess> wrapper = new LambdaQueryWrapper<FarmerAccess>()
                .eq(status != null, FarmerAccess::getStatus, status)
                .orderByDesc(FarmerAccess::getUpdateTime);
        Page<FarmerAccess> page = farmerAccessMapper.selectPage(Page.of(pageNo, pageSize), wrapper);
        return new PageResultVO<>(page.getTotal(), toVoList(page.getRecords()));
    }

    /**
     * 管理端通过申请：升为农户 + 写入/更新 farm_user
     * @param id 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        SecurityUtil.requireAdmin();
        FarmerAccess access = requirePendingAccess(id);
        access.unpackInfo();
        promoteUserToFarmer(access.getUserId());
        upsertFarmUser(access);
        markAccessApproved(access);
        log.info("农户申请通过，accessId={}, userId={}", id, access.getUserId());
    }

    /**
     * 管理端拒绝申请：仅改状态
     * @param id 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id) {
        SecurityUtil.requireAdmin();
        FarmerAccess access = requirePendingAccess(id);
        markAccessRejected(access);
        log.info("农户申请拒绝，accessId={}, userId={}", id, access.getUserId());
    }

    // ===================== 二级方法 =====================

    // 校验当前用户为游客并返回完整 User
    private User requireVisitorUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getRole() != RoleEnum.VISITOR) {
            throw new BusinessException("仅游客可申请成为农户");
        }
        return user;
    }

    // 校验申请必填项
    private void validateApplyPayload(FarmerAccessDTO dto) {
        if (dto == null) {
            throw new BusinessException("申请信息不能为空");
        }
        if (StrUtil.isBlank(dto.getIdCard())) {
            throw new BusinessException("身份证号不能为空");
        }
        if (dto.getVillageId() == null) {
            throw new BusinessException("请选择所属农村");
        }
        if (dto.getBusinessType() == null) {
            throw new BusinessException("请选择经营类型");
        }
    }

    // 校验村落存在
    private void requireVillageExists(Long villageId) {
        if (villageMapper.selectById(villageId) == null) {
            throw new BusinessException("所选村落不存在");
        }
    }

    // 已通过则禁止再申请
    private void assertNotAlreadyApproved(Long userId) {
        Long count = farmerAccessMapper.selectCount(new LambdaQueryWrapper<FarmerAccess>()
                .eq(FarmerAccess::getUserId, userId)
                .eq(FarmerAccess::getStatus, AccessStatusEnum.APPROVED));
        if (count != null && count > 0) {
            throw new BusinessException("您已通过农户审批，无需再次申请");
        }
    }

    // 组装用户快照（写入 info JSON）
    private UserCacheInfo buildUserCacheInfo(User user) {
        UserCacheInfo info = new UserCacheInfo();
        info.setUsername(user.getUsername());
        info.setPhone(user.getPhone());
        info.setEmail(user.getEmail());
        info.setAvatar(user.getAvatar());
        return info;
    }

    // 查本人最新待审单
    private FarmerAccess findLatestPending(Long userId) {
        return farmerAccessMapper.selectOne(new LambdaQueryWrapper<FarmerAccess>()
                .eq(FarmerAccess::getUserId, userId)
                .eq(FarmerAccess::getStatus, AccessStatusEnum.PENDING)
                .orderByDesc(FarmerAccess::getId)
                .last("limit 1"));
    }

    // 更新待审单（改提）
    private void updatePendingApply(FarmerAccess pending, FarmerAccessDTO dto, UserCacheInfo cacheInfo) {
        pending.setUserCacheInfo(cacheInfo);
        pending.packInfo();
        pending.setIdCard(dto.getIdCard().trim());
        pending.setVillageId(dto.getVillageId());
        pending.setBusinessType(dto.getBusinessType());
        farmerAccessMapper.updateById(pending);
    }

    // 新增待审单
    private void insertPendingApply(Long userId, FarmerAccessDTO dto, UserCacheInfo cacheInfo) {
        FarmerAccess access = new FarmerAccess();
        access.setUserId(userId);
        access.setUserCacheInfo(cacheInfo);
        access.packInfo();
        access.setIdCard(dto.getIdCard().trim());
        access.setVillageId(dto.getVillageId());
        access.setBusinessType(dto.getBusinessType());
        access.setStatus(AccessStatusEnum.PENDING);
        farmerAccessMapper.insert(access);
    }

    // 取待审申请
    private FarmerAccess requirePendingAccess(Long id) {
        FarmerAccess access = farmerAccessMapper.selectById(id);
        if (access == null) {
            throw new BusinessException("申请记录不存在");
        }
        if (access.getStatus() != AccessStatusEnum.PENDING) {
            throw new BusinessException("该申请已处理，无法重复操作");
        }
        return access;
    }

    // 将申请人角色升为农户
    private void promoteUserToFarmer(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("申请人不存在");
        }
        if (user.getRole() != RoleEnum.VISITOR) {
            throw new BusinessException("申请人当前不是游客，无法通过申请");
        }
        User update = new User();
        update.setId(userId);
        update.setRole(RoleEnum.FARMER);
        userMapper.updateById(update);
    }

    // 写入或更新 farm_user 档案
    private void upsertFarmUser(FarmerAccess access) {
        FarmerUser exist = farmerMapper.selectOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, access.getUserId())
                .last("limit 1"));
        if (exist == null) {
            FarmerUser fu = new FarmerUser();
            fu.setUserId(access.getUserId());
            fu.setVillageId(access.getVillageId());
            fu.setIdCard(access.getIdCard());
            fu.setBusinessType(access.getBusinessType());
            farmerMapper.insert(fu);
            return;
        }
        FarmerUser fu = new FarmerUser();
        fu.setId(exist.getId());
        fu.setVillageId(access.getVillageId());
        fu.setIdCard(access.getIdCard());
        fu.setBusinessType(access.getBusinessType());
        farmerMapper.updateById(fu);
    }

    // 标记申请通过
    private void markAccessApproved(FarmerAccess access) {
        FarmerAccess update = new FarmerAccess();
        update.setId(access.getId());
        update.setStatus(AccessStatusEnum.APPROVED);
        update.setAuditUserId(SecurityUtil.currentUserId());
        farmerAccessMapper.updateById(update);
    }

    // 标记申请拒绝
    private void markAccessRejected(FarmerAccess access) {
        FarmerAccess update = new FarmerAccess();
        update.setId(access.getId());
        update.setStatus(AccessStatusEnum.REJECTED);
        update.setAuditUserId(SecurityUtil.currentUserId());
        farmerAccessMapper.updateById(update);
    }

    // 解析村落名称
    private String resolveVillageName(Long villageId) {
        if (villageId == null) {
            return null;
        }
        VillageBase village = villageMapper.selectById(villageId);
        return village == null ? null : village.getName();
    }

    /**
     * 批量转 VO：解压 info 快照 + 批量查村落名
     */
    private List<FarmerAccessVO> toVoList(List<FarmerAccess> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        // 1. 把 info 字段 JSON 还原成 userCacheInfo 对象
        records.forEach(FarmerAccess::unpackInfo);
        // 2. 收集村落 ID，一次 IN 查询拿村名
        Set<Long> villageIds = records.stream()
                .map(FarmerAccess::getVillageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = villageIds.isEmpty()
                ? Collections.emptyMap()
                : villageMapper.selectByIds(villageIds).stream()
                .collect(Collectors.toMap(VillageBase::getId, VillageBase::getName, (a, b) -> a));
        // 3. 逐条转 VO 并挂上村名
        return records.stream()
                .map(r -> toVo(r, nameMap.get(r.getVillageId())))
                .collect(Collectors.toList());
    }

    // 单条转 VO
    private FarmerAccessVO toVo(FarmerAccess access, String villageName) {
        FarmerAccessVO vo = BeanUtil.copyProperties(access, FarmerAccessVO.class);
        vo.setUserCacheInfo(access.getUserCacheInfo());
        vo.setVillageName(villageName);
        return vo;
    }

    // 规范化页码
    private int normalizePageNo(PageResultDTO dto) {
        if (dto == null || dto.getPageNo() == null || dto.getPageNo() < 1) {
            return 1;
        }
        return dto.getPageNo();
    }

    // 规范化每页条数
    private int normalizePageSize(PageResultDTO dto) {
        if (dto == null || dto.getPageSize() == null || dto.getPageSize() < 1) {
            return 10;
        }
        return dto.getPageSize();
    }
}

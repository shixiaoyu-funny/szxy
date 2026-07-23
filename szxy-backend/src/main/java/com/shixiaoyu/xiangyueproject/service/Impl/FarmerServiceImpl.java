package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerUserDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.entity.po.*;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerAccessVO;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerUserVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ManagerAccessVO;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.mapper.*;
import com.shixiaoyu.xiangyueproject.service.FarmerService;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FarmerServiceImpl extends ServiceImpl<FarmerMapper, FarmerUser> implements FarmerService {
    private final FarmerMapper farmerMapper;
    private final VillageMapper villageMapper;
    private final ManagerAccessMapper managerAccessMapper;
    private final LoginMapper loginMapper; // 用于操作 user 表
    private final BCryptPasswordEncoder encoder; // 密码加密
    private final FarmerAccessMapper farmerAccessMapper;
    private final ScenicAccessMapper scenicAccessMapper;
    private final ScenicMapper scenicMapper;

    /**
     * 管理端资质/农户全量接口：仅 user.type = 管理员（3），与「超级管理员」无关
     */
    private boolean isAdminUser() {
        UserDTO u = UserHolder.getUser();
        return u != null && Integer.valueOf(CommonConstants.USER_TYPE_ADMIN).equals(u.getType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result applyManager(FarmerAccessDTO farmerAccessDTO) {
        // 1. 获取当前登录用户 ID（安全性：不使用前端传来的 userId）
        Long currentUserId = UserHolder.getUser().getId();
        // 2. 【核心校验】必须是农户（type=1）才能申请
        // 去 farm_user 表查该用户的记录
        FarmerUser farmer = this.getOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, currentUserId));
        if (farmer == null || !Integer.valueOf(1).equals(farmer.getType())) {
            return Result.error("权限不足：只有正式农户才能申请村长资质");
        }
        ManagerAccess application = new ManagerAccess();
        application.setUserId(currentUserId);
        application.setVillageId(farmer.getVillageId()); // 自动获取该农户所属的村，防止乱填
        application.setFarmName(farmerAccessDTO.getFarmName());
        application.setIdCard(farmerAccessDTO.getIdCard());
        // 设置初始状态
        application.setStatus(0); // 0-待审核
        // 5. 插入 manager_access 表
        managerAccessMapper.insert(application);

        log.info("农户 {} 已提交村长资质申请，目标村落: {}", currentUserId, farmer.getVillageId());
        return Result.ok("资质申请已提交，请等待管理员审核");
    }

    @Override
    public Result<List<FarmerUserVO>> getFarmersByVillage() {
        // 1. 获取当前登录用户的 ID
        Long currentUserId = UserHolder.getUser().getId();
        // 直接去 farm_user 表查：谁是该用户，且 type=2（村长）
        Long villageId = farmerMapper.getVillageIdByChiefId(currentUserId);
        if (villageId == null) {
            log.warn("用户 {} 尝试查看农户列表，但在 farm_user 表中未发现其具备 type=2 的村长身份", currentUserId);
            return Result.error("权限不足：您不是该村的管理员（村长）");
        }
        // 3. 拿到村子 ID 后，查询该村下所有 type=1（农户）的人
        // 建议把这个 Mapper 方法也改一下名，增加 type=1 的过滤，防止查出村长自己
        List<FarmerUserVO> list = farmerMapper.selectFarmersByVillageAndType(villageId);
        log.info("村长 {} 成功获取村落 {} 的农户列表，共 {} 条数据", currentUserId, villageId, list.size());
        return Result.ok(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 事务：确保 user 表和 farm_user 表同时成功
    public Result addFarmer(Long villageId, FarmerUserDTO farmerUserDTO) {
        // 1. 检查手机号是否已存在
        User existUser = loginMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, farmerUserDTO.getPhone()));
        if (existUser != null) {
            return Result.error("手机号 " + farmerUserDTO.getPhone() + " 已被占用");
        }

        // 2. 第一步：创建底层 User（账号信息）
        User user = new User();
        user.setUsername(farmerUserDTO.getFarmName());
        user.setPhone(farmerUserDTO.getPhone());
        user.setPassword(encoder.encode("123456")); // 初始默认密码 123456
        user.setStatus(1); // 1-正常
        user.setType(2);   // 2-农户（user.type：1 普通用户、2 农户、3 管理员）

        // 插入 user 表
        loginMapper.insert(user);

        // 关键：MyBatis-Plus 会自动将自增生成的 ID 回填到 user 对象中
        Long newUserId = user.getId();
        log.info("新用户账号创建成功，ID: {}", newUserId);

        // 3. 第二步：创建业务 FarmerUser（农户扩展信息）
        FarmerUser farmerUser = new FarmerUser();
        farmerUser.setUserId(newUserId);     // 绑定刚才生成的 User ID
        farmerUser.setVillageId(villageId);  // 绑定 Controller 传来的村落 ID
        farmerUser.setFarmName(farmerUserDTO.getFarmName());
        farmerUser.setType(farmerUserDTO.getType()); // 1-民宿, 2-农产品等
        farmerUser.setStatus(1); // 设置为已审核通过状态

        // 插入 farm_user 表
        this.save(farmerUser);

        log.info("农户关联村落成功：村落ID={}, 农场名={}", villageId, farmerUserDTO.getFarmName());

        return Result.ok("农户添加成功，初始密码为 123456");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateFarmer(Long id, FarmerUserDTO farmerUserDTO) {
        // 1. 获取当前登录村长的用户ID
        Long currentUserId = UserHolder.getUser().getId();
        // 2. 确定村长管辖的村落 ID
        Long chiefVillageId = farmerMapper.getVillageIdByManageId(currentUserId);
        if (chiefVillageId == null) {
            return Result.error("权限不足：您不是任何村落的管理员");
        }
        // 这里的 id 是前端传来的 @PathVariable 农户用户ID
        FarmerUser targetFarmer = this.getOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, id));
        if (targetFarmer == null) {
            return Result.error("该农户业务记录不存在");
        }
        if (!chiefVillageId.equals(targetFarmer.getVillageId())) {
            log.warn("越权操作警告！村长 {} 尝试修改非本村农户 {} 的资料", currentUserId, id);
            return Result.error("操作失败：该农户不属于您的管辖范围");
        }
        // --- 校验通过，执行更新逻辑 ---
        // 5. 更新 user 表 (基础信息)
        User user = loginMapper.selectById(id);
        if (user == null) return Result.error("该用户账号不存在");
        // 校验手机号冲突
        if (StrUtil.isNotBlank(farmerUserDTO.getPhone()) && !farmerUserDTO.getPhone().equals(user.getPhone())) {
            User check = loginMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, farmerUserDTO.getPhone()));
            if (check != null) return Result.error("手机号已被占用");
        }
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setUsername(farmerUserDTO.getUsername());
        updateUser.setPhone(farmerUserDTO.getPhone());
        loginMapper.updateById(updateUser);
        // 6. 更新 farm_user 表 (业务信息)
        LambdaUpdateWrapper<FarmerUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FarmerUser::getUserId, id)
                .set(FarmerUser::getFarmName, farmerUserDTO.getFarmName())
                .set(FarmerUser::getType, farmerUserDTO.getType());
        this.update(updateWrapper);
        log.info("村长 {} 成功修改了本村农户 {} 的资料", currentUserId, id);
        return Result.ok("修改成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 事务：确保删除关联和更新身份同时成功
    public Result deleteFarmer(Long id) {
        // 1. 获取当前登录村长的 ID 和管辖的村落 ID
        Long chiefUserId = UserHolder.getUser().getId();
        Long chiefVillageId = farmerMapper.getVillageIdByChiefId(chiefUserId);

        if (chiefVillageId == null) {
            return Result.error("权限不足：您不是村落管理员");
        }

        // 2. 查找目标农户的业务记录
        FarmerUser targetFarmer = this.getOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, id));

        if (targetFarmer == null) {
            return Result.error("该农户业务记录不存在");
        }

        // 3. 【核心权限检查】只能删除自己村里的农户
        if (!chiefVillageId.equals(targetFarmer.getVillageId())) {
            log.warn("越权删除警告！村长 {} 尝试删除非本村农户 {}", chiefUserId, id);
            return Result.error("操作失败：该农户不属于您的管辖范围");
        }

        // 4. 第一步：删除 farm_user 表中的关联数据
        // 这样他在“本村农户列表”里就消失了
        this.remove(new LambdaQueryWrapper<FarmerUser>().eq(FarmerUser::getUserId, id));

        // 5. 第二步：修改 user 表中的身份类型
        // 虽然保留了账号，但要把他的 type 改回 1 (普通用户)，不再是 2 (农户)
        User user = new User();
        user.setId(id);
        user.setType(1);
        loginMapper.updateById(user);

        log.info("村长 {} 已将用户 {} 从本村农户名单中移除", chiefUserId, id);
        return Result.ok("农户移除成功，该账号已转为普通用户");
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 必须加事务，保证四张表同步更新
    public Result setVillageManager(Long villageId, Long farmerId) {
        // 1. 获取申请记录并校验 (manager_access 表)
        ManagerAccess access = managerAccessMapper.selectOne(new LambdaQueryWrapper<ManagerAccess>()
                .eq(ManagerAccess::getUserId, farmerId)
                .eq(ManagerAccess::getVillageId, villageId)
                .eq(ManagerAccess::getStatus, 0));
        if (access == null) return Result.error("未找到有效的待审批申请记录");

        // 2. 获取【当前村落】的信息 (village_base 表)
        VillageBase village = villageMapper.selectById(villageId);
        if (village == null) return Result.error("该村落不存在");

        // --- 关键点：提取旧管理员 ID ---
        // 这个 manage_id 就是你说的“旧的管理员 ID”
        Long oldManagerId = village.getManageId();
        log.info("执行村长更替：村落={}, 旧村长ID={}, 新村长ID={}", villageId, oldManagerId, farmerId);

        // 3. 【降级】如果这个村子之前有管理员，且不是同一个人，就把他降为农户
        if (oldManagerId != null && !oldManagerId.equals(farmerId)) {
            // 去 farm_user 表，把旧管理员的 type 改为 1
            managerAccessMapper.demoteOldManager(oldManagerId, villageId);

            // 同时：把旧管理员在全局 user 表的 type 改回 2 (农户)
            // (可选逻辑，根据你全局身份定义来)
            User oldUser = new User();
            oldUser.setId(oldManagerId);
            oldUser.setType(2);
            loginMapper.updateById(oldUser);
        }

        // 4. 【审批通过】修改申请表状态
        access.setStatus(1);
        managerAccessMapper.updateById(access);

        // 5. 【任命】修改村落表的管理员 (village_base 表的 manage_id 字段)
        village.setManageId(farmerId);
        villageMapper.updateById(village);

        // 6. 【升级新村长业务身份】修改 farm_user 表，type 改为 2
        managerAccessMapper.promoteNewManager(farmerId, villageId);

        // 7. 【升级新村长全局身份】修改 user 表，type 改为 3 (管理员)
        User newUser = new User();
        newUser.setId(farmerId);
        newUser.setType(3);
        loginMapper.updateById(newUser);

        return Result.ok("村长任命已生效");
    }

    @Override
    public Result getFarmerAccessList() {
        // 1. 获取当前登录用户的 ID
        Long currentUserId = UserHolder.getUser().getId();

        // 2. 获取该用户作为“村长”关联的村落 ID
        // (复用之前的 getVillageIdByChiefId 方法，逻辑是查 farm_user 表 type=2)
        Long villageId = farmerMapper.getVillageIdByChiefId(currentUserId);

        // 3. 权限判断
        if (villageId == null) {
            log.warn("用户 {} 尝试查看申请列表，但他不是村长", currentUserId);
            return Result.error("权限不足：只有本村村长可查看申请列表");
        }

        // 4. 只查询该村子下的申请记录
        List<FarmerAccessVO> list = farmerMapper.selectFarmerAccessByVillage(villageId);

        log.info("村长 {} 正在查看村落 {} 的申请列表，共 {} 条", currentUserId, villageId, list.size());
        return Result.ok(list);
    }

    @Override
    public Result getManagerAccessList() {
        UserDTO currentUser = UserHolder.getUser();
        if (!isAdminUser()) {
            log.warn("越权访问：用户 {} 尝试获取村长资质申请列表", currentUser != null ? currentUser.getId() : null);
            return Result.error("权限不足：仅管理员可查看此列表");
        }

        List<ManagerAccessVO> list = managerAccessMapper.selectManagerAccessList();

        log.info("管理员 {} 成功获取村长资质申请列表，共 {} 条", currentUser.getId(), list.size());
        return Result.ok(list);
    }

    @Override
    public Result<List<FarmerUserVO>> getAllFarmers() {
        UserDTO currentUser = UserHolder.getUser();
        if (!isAdminUser()) {
            log.warn("越权访问：用户 {} 尝试获取所有农户列表", currentUser != null ? currentUser.getId() : null);
            return Result.error("权限不足：仅管理员可查看此列表");
        }

        List<FarmerUserVO> list = farmerMapper.selectAllFarmers();

        log.info("管理员 {} 成功获取所有农户列表，共 {} 条", currentUser.getId(), list.size());
        return Result.ok(list);
    }

    @Override
    public Result getAllFarmerAccessList() {
        UserDTO currentUser = UserHolder.getUser();
        if (!isAdminUser()) {
            log.warn("越权访问：用户 {} 尝试获取所有农户资质申请列表", currentUser != null ? currentUser.getId() : null);
            return Result.error("权限不足：仅管理员可查看此列表");
        }

        List<FarmerAccessVO> list = farmerMapper.selectAllFarmerAccess();

        log.info("管理员 {} 成功获取所有农户资质申请列表，共 {} 条", currentUser.getId(), list.size());
        return Result.ok(list);
    }

    @Transactional
    @Override
    public Result solveFarmer(Long id, Integer status, Long villageId) {
        if (!isAdminUser()) {
            return Result.error("权限不足：仅管理员可审批农户资质申请");
        }
        FarmerAccess record = farmerAccessMapper.selectById(id);
        if (record == null) {
            return Result.error("申请记录不存在");
        }
        if (status == null || (status != 1 && status != 2)) {
            return Result.error("无效的处理状态");
        }
        if (status == 2) {
            record.setStatus(2);
            farmerAccessMapper.updateById(record);
            log.info("农户资质申请已拒绝 {}", id);
            return Result.ok();
        }
        Long effectiveVillageId = villageId != null ? villageId : record.getVillageId();
//        if (effectiveVillageId == null) {
//            return Result.error("通过审批前须指定或补齐所属村落（请让用户在农户端绑定农村）");
//        }
        FarmerUser row = new FarmerUser();
        row.setUserId(record.getUserId());
        row.setVillageId(effectiveVillageId);
        row.setFarmName(record.getFarmName());
        row.setIdCard(record.getIdCard());
        row.setType(record.getType());
        row.setStatus(1);
        LambdaQueryWrapper<FarmerUser> wrapper = new LambdaQueryWrapper<FarmerUser>().eq(FarmerUser::getUserId, record.getUserId());
        FarmerUser existing = farmerMapper.selectOne(wrapper);
        if (existing != null) {
            row.setId(existing.getId());
            farmerMapper.updateById(row);
        } else {
            farmerMapper.insert(row);
        }
        User user = loginMapper.selectById(record.getUserId());
        if (user == null) {
            log.warn("审批农户申请时用户不存在 userId={}", record.getUserId());
            return Result.error("关联用户不存在");
        }
        user.setType(2);
        loginMapper.updateById(user);
        if (villageId == null || villageId==0) {
            record.setStatus(3);
            farmerAccessMapper.updateById(record);
            return Result.ok();
        }
        farmerAccessMapper.deleteById(id);
        log.info("农户资质申请已通过并写入 farm_user，userId={}", record.getUserId());
        return Result.ok();
    }

    @Override
    public Result solveManager(Long id, Integer status) {
        if (!isAdminUser()) {
            return Result.error("权限不足：仅管理员可审批村长资质申请");
        }
        ManagerAccess managerAccess = managerAccessMapper.selectById(id);
        managerAccess.setId(null);
        if (status == 1) {
            Long villageId = managerAccess.getVillageId();
            VillageBase villageBase = villageMapper.selectById(villageId);
            if (villageBase.getManageId() != null) {
                log.info("该村落{}已存在村长", villageId);
                managerAccess.setStatus(2);
                managerAccessMapper.updateById(managerAccess);
            } else {
                villageBase.setManageId(managerAccess.getUserId());
                villageMapper.updateById(villageBase);
                log.info("该村落{}申请村长{}成功", villageId, managerAccess.getUserId());
                managerAccess.setStatus(1);
                managerAccessMapper.deleteById(id);
            }
        } else {
            log.info("该村落{}申请村长{}被拒绝", managerAccess.getVillageId(), managerAccess.getUserId());
            managerAccess.setStatus(2);
            managerAccessMapper.updateById(managerAccess);
        }
        return Result.ok();
    }

    @Override
    public Result solveScenic(Long id, Integer status) {
        if (!isAdminUser()) {
            return Result.error("权限不足：仅管理员可审批景点资质申请");
        }
        ScenicAccess scenicAccess = scenicAccessMapper.selectById(id);
        scenicAccess.setStatus(status);
        if (status == 1) {
            VillageScenic villageScenic = BeanUtil.copyProperties(scenicAccess, VillageScenic.class);
            villageScenic.setId(null);
            log.info("插入村落{}的景点", villageScenic);
            scenicMapper.insert(villageScenic);
            log.info("插入村落{}的景点{}成功", villageScenic.getVillageId(), villageScenic.getName());
        }
        scenicAccessMapper.deleteById(scenicAccess);
        return Result.ok();
    }
}

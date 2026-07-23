package com.shixiaoyu.xiangyueproject.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerUserDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.ManagerAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.ScenicAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerAccess;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerUserVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.mapper.*;
import com.shixiaoyu.xiangyueproject.server.WebSocketServer;
import com.shixiaoyu.xiangyueproject.service.FarmerService;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/farmer")
@Tag(name = "农户接口")
@RequiredArgsConstructor
public class FarmerController {
    private final FarmerMapper farmerMapper;
    private final FarmerService farmerService;
    private final VillageMapper villageMapper;
    private final ScenicMapper scenicMapper;
    private final FarmerAccessMapper farmerAccessMapper;
    private final LoginMapper loginMapper;
    private final WebSocketServer webSocketServer;


    /**
     * 农户申请村长资质
     * @param farmerAccessDTO
     * @return
     */
    @PostMapping("/apply_manager")
    @Operation(summary = "申请村长资质")
    public Result applyManager(@Parameter(description = "申请村长信息",name = "farmerAccessDTO",required = true) @RequestBody FarmerAccessDTO farmerAccessDTO){
        webSocketServer.sendToAllClient("manager_access");
        return farmerService.applyManager(farmerAccessDTO);
    }

    @PostMapping("/apply_farmer")
    @Operation(summary = "申请农户资质")
    @Transactional
    public Result applyFarmer(@Parameter(description = "申请农户信息",name = "farmerAccessDTO",required = true) @RequestBody FarmerAccessDTO farmerAccessDTO){
        Long currentUserId = UserHolder.getUser().getId();
        farmerAccessDTO.setUserId(currentUserId);
        String raw = StrUtil.trimToEmpty(farmerAccessDTO.getVillageName());
        if (StrUtil.isBlank(raw)) {
            return Result.error("请填写农村名称或村落编号");
        }
        VillageBase villageBase = null;
        if (StrUtil.isNumeric(raw)) {
            villageBase = villageMapper.selectById(Long.parseLong(raw));
        }
        if (villageBase == null) {
            LambdaQueryWrapper<VillageBase> wrapper = new LambdaQueryWrapper<VillageBase>().eq(VillageBase::getName, raw);
            villageBase = villageMapper.selectOne(wrapper);
        }
        if (villageBase == null) {
            return Result.error("该村落不存在");
        }
        // 用户端已先创建 farmer_access；此处仅更新当前用户的待审记录，并绑定村落（此时 farm_user 尚未存在，不能查 farm_user）
        FarmerAccess access = farmerAccessMapper.selectOne(
                new LambdaQueryWrapper<FarmerAccess>()
                        .eq(FarmerAccess::getUserId, currentUserId)
                        .orderByDesc(FarmerAccess::getId)
                        .last("LIMIT 1"));
        if (access == null) {
            return Result.error("未找到待审核的农户申请，请先在用户端提交农户资质申请");
        }
        if (!StrUtil.equals(StrUtil.trim(access.getFarmName()), StrUtil.trim(farmerAccessDTO.getFarmName()))
                || !StrUtil.equals(StrUtil.trim(access.getIdCard()), StrUtil.trim(farmerAccessDTO.getIdCard()))
                || !Objects.equals(access.getType(), farmerAccessDTO.getType())) {
            return Result.error("农户姓名、身份证或经营类型须与首次申请时一致");
        }
        access.setVillageId(villageBase.getId());
        access.setStatus(0);
        log.info("farmerAccess update village: {}", access);
        farmerAccessMapper.updateById(access);
        webSocketServer.sendToAllClient("farmer_access");
        return Result.ok();
    }

    /**
     * 本村农户列表
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "本村农户列表")
    public Result<List<FarmerUserVO>> getFarmers(){
        return farmerService.getFarmersByVillage();
    }

    /**
     * 农村下新增农户
     * @param villageId
     * @param farmerUserDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "农村下新增农户")
    public Result addFarmer(
            @Parameter(description = "农村id",name = "villageId",required = true,in = ParameterIn.QUERY) @RequestParam Long villageId,
            @RequestBody FarmerUserDTO farmerUserDTO){
        return farmerService.addFarmer(villageId,farmerUserDTO);
    }

    /**
     * 修改农村下属农户信息
     * @param id
     * @param farmerUserDTO
     * @return
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改农村下属农户信息")
    public Result updateFarmer(
            @Parameter(description = "农户id",name = "id",required = true,in = ParameterIn.PATH) @PathVariable Long id,
            @RequestBody FarmerUserDTO farmerUserDTO){
        return farmerService.updateFarmer(id, farmerUserDTO);
    }

    /**
     * 删除农村下属农户
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除农村下属农户")
    public Result deleteFarmer(
            @Parameter(description = "农户id",name = "id",required = true,in = ParameterIn.PATH) @PathVariable Long id){
        return farmerService.deleteFarmer(id);
    }

    /**
     * 标记/修改村长
     * @param villageId
     * @param farmerId
     * @return
     */
    @PostMapping("/set-manager")
    @Operation(summary = "标记/修改农村村长")
    public Result setVillageManager(
            @Parameter(description = "农村id",name = "villageId",required = true,in = ParameterIn.QUERY) @RequestParam Long villageId,
            @Parameter(description = "农户id（村长）",name = "farmerId",required = true,in = ParameterIn.QUERY) @RequestParam Long farmerId){
        return farmerService.setVillageManager(villageId,farmerId);
    }

    @GetMapping("/village")
    @Operation(summary = "查询当前农户所属农村信息")
    public Result getVillageById(){
        LambdaQueryWrapper<FarmerUser> wrapper = new LambdaQueryWrapper<FarmerUser>().eq(FarmerUser::getUserId, UserHolder.getUser().getId());
        FarmerUser farmerUser = farmerMapper.selectOne(wrapper);
        if(farmerUser==null){
            log.info("查询当前农户所属村信息失败：{}",farmerUser);
            return Result.ok();
        }
        Long villageId = farmerUser.getVillageId();
        VillageBase villageBase = villageMapper.selectById(villageId);
        if(villageBase==null){
            log.info("查询当前农户所属村信息失败：{}",villageId);
            return Result.ok();
        }
        log.info("查询当前农户所属村信息：{}",villageBase);
        VillageBaseVO vo = BeanUtil.copyProperties(villageBase, VillageBaseVO.class);
        // manage_id 存的是村长对应 user.id，VO 需展示 managerName，不能仅靠 copy
        fillManagerName(vo, villageBase);
        log.info("查询当前农户所属村信息VO：{}", vo);
        return Result.ok(vo);
    }

    /**
     * 根据 village_base.manage_id（村长用户 id）解析村长展示名：优先 farm_user.farm_name，否则 user.username
     */
    private void fillManagerName(VillageBaseVO vo, VillageBase villageBase) {
        Long manageUserId = villageBase.getManageId();
        if (manageUserId == null) {
            return;
        }
        FarmerUser chief = farmerMapper.selectOne(
                new LambdaQueryWrapper<FarmerUser>().eq(FarmerUser::getUserId, manageUserId).last("LIMIT 1"));
        log.info("根据 village_base.manage_id（村长用户 id）解析村长展示名：{}", chief);
        if (chief != null && StrUtil.isNotBlank(chief.getFarmName())) {
            vo.setManagerName(chief.getFarmName());
            return;
        }
        User user = loginMapper.selectById(manageUserId);
        if (user != null && StrUtil.isNotBlank(user.getUsername())) {
            vo.setManagerName(user.getUsername());
        }
    }

    @GetMapping("/scenic")
    @Operation(summary = "查询当前农户下属景点信息")
    public Result getScenicById(){
        LambdaQueryWrapper<VillageScenic> wrapper = new LambdaQueryWrapper<VillageScenic>().eq(VillageScenic::getUserId, UserHolder.getUser().getId());
        List<VillageScenic> villageScenics = scenicMapper.selectList(wrapper);
        List<ScenicVO> voList = villageScenics.stream().map(item -> {
            ScenicVO vo = BeanUtil.copyProperties(item, ScenicVO.class);
            fillScenicVillageName(vo, item.getVillageId());
            return vo;
        }).toList();
        log.info("查询当前农户下属景点信息：{}",voList);
        return Result.ok(voList);
    }

    /** ScenicVO.villageName 为展示用村名，需由 village_base 根据 villageId 填充 */
    private void fillScenicVillageName(ScenicVO vo, Long villageId) {
        if (villageId == null) {
            return;
        }
        VillageBase vb = villageMapper.selectById(villageId);
        if (vb != null && StrUtil.isNotBlank(vb.getName())) {
            vo.setVillageName(vb.getName());
        }
    }
}

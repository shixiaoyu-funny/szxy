package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.FarmerUserDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerUserVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.service.FarmerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 农户接口（村长操作本村农户 + 管理端建档/任命）
 * 管理端方法（/fmr/all、/fmr/create、/fmr/set-manager）由 Service 层 SecurityUtils.requireAdmin() 校验 role=4
 */
@Slf4j
@RestController
@RequestMapping("/fmr")
@Tag(name = "农户接口")
@RequiredArgsConstructor
public class FarmerController {
    private final FarmerService farmerService;

    @GetMapping("/ls")
    @Operation(summary = "本村农户列表（村长）")
    public Result<List<FarmerUserVO>> getFarmers() {
        return Result.ok(farmerService.getFarmersByVillage());
    }

    @PostMapping("/new")
    @Operation(summary = "新增本村农户（村长）", description = "便于村长直接增加农户")
    public Result<Void> addFarmer(
            @Parameter(description = "农村id", name = "village_id", required = true, in = ParameterIn.QUERY) @RequestParam Long villageId,
            @Parameter(description = "农户信息（user基础信息 + farmer档案信息）", name = "farmerUserDTO", required = true) @RequestBody FarmerUserDTO farmerUserDTO) {
        farmerService.addFarmer(villageId, farmerUserDTO);
        return Result.ok();
    }

    @PostMapping("/modify/{id}")
    @Operation(summary = "修改本村农户（村长）")
    public Result<Void> updateFarmer(
            @Parameter(description = "农户 user.id", name = "id", required = true, in = ParameterIn.PATH) @PathVariable Long id,
            @Parameter(description = "农户信息（user基础信息 + farmer档案信息）", name = "farmerUserDTO", required = true) @RequestBody FarmerUserDTO farmerUserDTO) {
        farmerService.updateFarmer(id, farmerUserDTO);
        return Result.ok();
    }

    @PostMapping("/remote/{id}")
    @Operation(summary = "删除本村农户（档案删除，账号降为游客；村长）")
    public Result<Void> deleteFarmer(
            @Parameter(description = "农户 user.id", name = "id", required = true, in = ParameterIn.PATH) @PathVariable Long id) {
        farmerService.deleteFarmer(id);
        return Result.ok();
    }

    @GetMapping("/vlg")
    @Operation(summary = "我的所属农村信息（村长/农户）")
    public Result<VillageBaseVO> myVillage() {
        return Result.ok(farmerService.getMyVillage());
    }

    @GetMapping("/sc")
    @Operation(summary = "我创建的景点列表（村长/农户）")
    public Result<List<ScenicVO>> myScenics() {
        return Result.ok(farmerService.getMyScenics());
    }

    // ==================== 管理端（Service 层 SecurityUtils.requireAdmin() 校验 role=4） ====================

    @GetMapping("/all")
    @Operation(summary = "管理端：获取所有农户列表")
    public Result<List<FarmerUserVO>> getAllFarmers() {
        return Result.ok(farmerService.getAllFarmers());
    }

    @PostMapping("/create")
    @Operation(summary = "管理端：建档农户（创建账号+档案，默认密码 123456）")
    public Result<Void> createFarmer(
            @Parameter(description = "农村id", name = "village_id", required = true, in = ParameterIn.QUERY) @RequestParam Long villageId,
            @Parameter(description = "农户信息（user基础信息 + farmer档案信息）", name = "farmerUserDTO", required = true) @RequestBody FarmerUserDTO farmerUserDTO) {
        farmerService.createFarmer(villageId, farmerUserDTO);
        return Result.ok();
    }

    @PostMapping("/set-manager")
    @Operation(summary = "管理端：任命/更换村长（事务内同步 manage_id 与角色）")
    public Result<Void> setManager(
            @Parameter(description = "农村id", name = "village_id", required = true, in = ParameterIn.QUERY) @RequestParam Long villageId,
            @Parameter(description = "农户 user.id", name = "farmer_user_id", required = true, in = ParameterIn.QUERY) @RequestParam Long farmerUserId) {
        farmerService.setVillageManager(villageId, farmerUserId);
        return Result.ok();
    }
}

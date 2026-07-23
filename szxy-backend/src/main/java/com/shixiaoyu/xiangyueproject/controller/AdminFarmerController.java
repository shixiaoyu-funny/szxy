package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerUserVO;
import com.shixiaoyu.xiangyueproject.service.FarmerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/farmer")
@Tag(name = "农户接口（管理端）")
@RequiredArgsConstructor
public class AdminFarmerController {
    private final FarmerService farmerService;

    /**
     * 获取所有农户列表
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有农户列表")
    public Result<List<FarmerUserVO>> getAllFarmers() {
        return farmerService.getAllFarmers();
    }

    /**
     * 获取所有农户资质申请列表
     * @return
     */
    @GetMapping("/farmer_access")
    @Operation(summary = "获取所有农户资质申请列表")
    public Result getAllFarmerAccessList() {
        return farmerService.getAllFarmerAccessList();
    }

    /**
     * 获取所有村长资质申请列表
     * @return
     */
    @GetMapping("/manager_access")
    @Operation(summary = "获取所有村长资质申请列表")
    public Result getAllManagerAccessList() {
        // 这里可以直接调用现有的 getManagerAccessList 方法，因为它已经是超管权限
        return farmerService.getManagerAccessList();
    }

    /**
     * 处理农户资质申请
     * @param id
     * @param status
     * @return
     */
    @PostMapping("/farmer_access/{id}")
    @Operation(summary = "处理农户资质申请")
    public Result applyFarmer(@Parameter(description = "申请表id") @PathVariable Long id,
                              @Parameter(description = "处理结果：1 通过、2 拒绝") @RequestParam Integer status,
                              @Parameter(description = "农村 id（通过时必填，若申请已绑定村落可省略）") @RequestParam(required = false) Long villageId) {
        return farmerService.solveFarmer(id,status,villageId);
    }

    /**
     * 处理村长资质申请
     * @param id
     * @param status
     * @return
     */
    @PostMapping("/manager_access/{id}")
    @Operation(summary = "处理村长资质申请")
    public Result applyManager(@Parameter(description = "申请表id") @PathVariable Long id,@Parameter(description = "处理结果") @RequestParam Integer status) {
        return farmerService.solveManager(id,status);
    }

    /**
     * 处理景区资质申请
     * @param id
     * @param status
     * @return
     */
    @PostMapping("/scenic_access/{id}")
    @Operation(summary = "处理景区资质申请")
    public Result applyScenic(@Parameter(description = "申请表id") @PathVariable Long id,@Parameter(description = "处理结果") @RequestParam Integer status) {
        return farmerService.solveScenic(id,status);
    }
}

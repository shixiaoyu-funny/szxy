package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.VillageBaseDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageDetailVO;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * 农村信息接口
 */
@Slf4j
@RestController
@RequestMapping("/vlg")
@Tag(name = "农村信息接口")
@RequiredArgsConstructor
public class VillageController {
    private final VillageService villageService;

    @GetMapping("/ls")
    @Operation(summary = "分页查询农村信息")
    public Result<PageResultVO<VillageBaseVO>> villageList(PageResultDTO pageResultDTO) {
        return Result.ok(villageService.villageList(pageResultDTO));
    }

    @GetMapping("/detail")
    @Operation(summary = "农村详情")
    public Result<VillageDetailVO> detail(
            @Parameter(description = "农村id", required = true) @RequestParam Long id) {
        return Result.ok(villageService.detail(id));
    }

    @PostMapping("/new")
    @Operation(summary = "新增农村")
    public Result<Void> addVillage(
            @Parameter(description = "农村信息", required = true) @RequestBody VillageBaseDTO villageBaseDTO) {
        villageService.addVillage(villageBaseDTO);
        return Result.ok();
    }

    @PostMapping("/modify/{id}")
    @Operation(summary = "修改农村信息")
    public Result<Void> updateVillage(
            @Parameter(description = "农村id", required = true) @PathVariable Long id,
            @Parameter(description = "农村信息", required = true) @RequestBody VillageBaseDTO villageBaseDTO) {
        villageService.updateVillage(id, villageBaseDTO);
        return Result.ok();
    }

    @PostMapping("/del/{id}")
    @Operation(summary = "删除农村")
    public Result<Void> deleteVillage(
            @Parameter(description = "农村id", required = true) @PathVariable Long id) {
        villageService.deleteVillage(id);
        return Result.ok();
    }

    @GetMapping("/likes")
    @Operation(summary = "优质农村 top10（按下属景点总点赞量）")
    public Result<List<VillageBaseVO>> villageLikes() {
        return Result.ok(villageService.villageLikes());
    }

    @GetMapping("/collections")
    @Operation(summary = "优质农村 top10（按下属景点总收藏量）")
    public Result<List<VillageBaseVO>> villageCollections() {
        return Result.ok(villageService.villageCollections());
    }
}

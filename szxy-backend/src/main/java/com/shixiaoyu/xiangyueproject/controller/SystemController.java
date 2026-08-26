package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.SystemConfigDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.SystemConfigVO;
import com.shixiaoyu.xiangyueproject.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置（管理端 CRUD）
 */
@RestController
@RequestMapping("/sys")
@Slf4j
@Tag(name = "系统配置接口")
@RequiredArgsConstructor
public class SystemController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/list")
    @Operation(summary = "管理端：配置分页列表")
    public Result<PageResultVO<SystemConfigVO>> list(
            PageResultDTO pageResultDTO,
            @Parameter(description = "配置键模糊搜索") @RequestParam(required = false) String keyword) {
        return Result.ok(systemConfigService.list(pageResultDTO, keyword));
    }

    @GetMapping("/{configKey}")
    @Operation(summary = "管理端：配置详情")
    public Result<SystemConfigVO> detail(
            @Parameter(description = "配置键", required = true) @PathVariable String configKey) {
        return Result.ok(systemConfigService.detail(configKey));
    }

    @PostMapping
    @Operation(summary = "管理端：新增配置")
    public Result<Void> create(@Valid @RequestBody SystemConfigDTO dto) {
        systemConfigService.create(dto);
        return Result.ok();
    }

    @PutMapping
    @Operation(summary = "管理端：更新配置")
    public Result<Void> update(@Valid @RequestBody SystemConfigDTO dto) {
        systemConfigService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{configKey}")
    @Operation(summary = "管理端：删除配置")
    public Result<Void> delete(
            @Parameter(description = "配置键", required = true) @PathVariable String configKey) {
        systemConfigService.delete(configKey);
        return Result.ok();
    }
}

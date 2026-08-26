package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.PositionDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.PositionVO;
import com.shixiaoyu.xiangyueproject.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 省市区经纬度缓存接口（仅 Redis，不直接调高德）。
 * 前端流程：getPos → 未命中则调高德 → savePos 回写缓存。
 */
@Slf4j
@RestController
@RequestMapping("/pos")
@RequiredArgsConstructor
@Tag(name = "位置经纬度缓存")
public class PositionController {

    private final PositionService positionService;

    /**
     * 从 Redis 查询经纬度。
     * key = pos:省:市:区；未命中时 data.longitude / data.latitude 为 null。
     */
    @Operation(summary = "按省市区查询经纬度缓存（未命中返回 null）")
    @GetMapping("/getPos")
    public Result<PositionVO> getPos(
            @Parameter(description = "省名", required = true) @RequestParam String province,
            @Parameter(description = "市名", required = true) @RequestParam String city,
            @Parameter(description = "区/县名", required = true) @RequestParam String county) {
        return Result.ok(positionService.getPos(province, city, county));
    }

    /**
     * 将前端（或高德）得到的经纬度写入 Redis。
     * value = 经度,纬度；不设过期时间。
     */
    @Operation(summary = "保存省市区经纬度到 Redis")
    @PostMapping("/savePos")
    public Result<Void> savePos(@RequestBody PositionDTO dto) {
        positionService.savePos(dto);
        return Result.ok();
    }
}

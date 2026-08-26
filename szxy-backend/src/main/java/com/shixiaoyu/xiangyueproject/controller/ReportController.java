package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.service.ReportService;
import com.shixiaoyu.xiangyueproject.utils.PVUVUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 报表接口（管理端）
 */
@Slf4j
@RestController
@RequestMapping("/report")
@Tag(name = "报表接口（管理端）")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final PVUVUtil pvuvUtil;

    @GetMapping("/farm")
    @Operation(summary = "农户数量")
    public Result<Integer> farmCnt() {
        return Result.ok(reportService.farmCnt());
    }

    @GetMapping("/uv")
    @Operation(summary = "当天UV")
    public Result<Long> uv() {
        return Result.ok(pvuvUtil.getUV(LocalDate.now()));
    }

    @GetMapping("/pv")
    @Operation(summary = "当天PV")
    public Result<Long> pv() {
        return Result.ok(pvuvUtil.getPV(LocalDate.now()));
    }

    @GetMapping("/uvpv")
    @Operation(summary = "网站粘性（PV/UV）")
    public Result<Double> uvpv() {
        long pv = pvuvUtil.getPV(LocalDate.now());
        long uv = pvuvUtil.getUV(LocalDate.now());
        if (uv == 0) {
            return Result.ok(0.0);
        }
        return Result.ok(pv / (double) uv);
    }

    @GetMapping("/scenic")
    @Operation(summary = "当日新增景点数量")
    public Result<Integer> newScenicCnt() {
        return Result.ok(reportService.newScenicCount());
    }

    @GetMapping("/village")
    @Operation(summary = "当日新增农村数量")
    public Result<Integer> newVillageCnt() {
        return Result.ok(reportService.newVillageCount());
    }

    @GetMapping("/pv7")
    @Operation(summary = "近7天PV走势")
    public Result<List<Long>> pv7() {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            list.add(pvuvUtil.getPV(LocalDate.now().minusDays(i)));
        }
        return Result.ok(list);
    }

    @GetMapping("/uv7")
    @Operation(summary = "近7天UV走势")
    public Result<List<Long>> uv7() {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            list.add(pvuvUtil.getUV(LocalDate.now().minusDays(i)));
        }
        return Result.ok(list);
    }
}

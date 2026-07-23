package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.service.ReportService;
import com.shixiaoyu.xiangyueproject.util.PVUVUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/report")
@Tag(name = "报表接口（管理端）")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final PVUVUtils pvuvUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    /**
     * 农户数量查询
     * @return
     */
    @GetMapping("/farm")
    @Operation(summary = "农户数量查询")
    public Result<Integer> farmCnt(){
        return reportService.farmCnt();
    }

    /**
     * 当天uv查询
     * @return
     */
    @GetMapping("/uv")
    @Operation(summary = "当天uv查询")
    public Result<Long> passengerFlow(){
        Long res = pvuvUtils.getUV(LocalDate.now());
        return Result.ok(res);
    }

    /**
     * 当天pv查询
     * @return
     */
    @GetMapping("/pv")
    @Operation(summary = "当天pv查询")
    public Result<Long> pv(){
        Long res = pvuvUtils.getPV(LocalDate.now());
        return Result.ok(res);
    }

    /**
     * 网站粘性查询
     * @return
     */
    @GetMapping("/uvpv")
    @Operation(summary = "网站粘性查询")
    public Result<Double> uvpv(){
        if(pvuvUtils.getPV(LocalDate.now()) == 0){
            return Result.ok(0.0);
        }
        return Result.ok((double)(pvuvUtils.getPV(LocalDate.now()) / pvuvUtils.getUV(LocalDate.now())));
    }

    /**
     * 新增景区数量查询
     * @return
     */
    @GetMapping("/scenic")
    @Operation(summary = "新增景区数量查询")
    public Result<Long> newScenicCnt(){
        Long size = stringRedisTemplate.opsForHyperLogLog().size("new:scenic" + LocalDate.now().format(DATE_FORMAT));
        return Result.ok(size);
    }

    /**
     * 新增农村数量查询
     * @return
     */
    @GetMapping("/village")
    @Operation(summary = "新增农户数量查询")
    public Result<Long> newFarmCnt(){
        LocalDate now = LocalDate.now();
        Long size = stringRedisTemplate.opsForHyperLogLog().size("new:village" + now.format(DATE_FORMAT));
        return Result.ok(size);
    }

    @GetMapping("/pv7")
    @Operation(summary = "近7天pv走势查询")
    public Result<List<Long>> pv7(){
        List<Long> list=new ArrayList<>();
        for(int i = 0; i < 7; i++){
            Long pv = pvuvUtils.getPV(LocalDate.now().minusDays(i));
            list.add(pv);
        }
        return Result.ok(list);
    }
    @GetMapping("/uv7")
    @Operation(summary = "近7天uv走势查询")
    public Result<List<Long>> uv7(){
        List<Long> list=new ArrayList<>();
        for(int i = 0; i < 7; i++){
            Long uv = pvuvUtils.getUV(LocalDate.now().minusDays(i));
            list.add(uv);
        }
        return Result.ok(list);
    }
}

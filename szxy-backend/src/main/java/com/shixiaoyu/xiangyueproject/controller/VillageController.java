package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.constants.ErrorConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import com.shixiaoyu.xiangyueproject.entity.dto.VillageBaseDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RequestMapping("/admin/village")
@RestController
@Tag(name = "农村信息接口（管理端）")
@RequiredArgsConstructor
public class VillageController {
    private final VillageService villageService;
    private final StringRedisTemplate stringRedisTemplate;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 分页查询农村信息
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询全部农村信息")
    public Result<PageResultVO<VillageBaseVO>> villageList(@Parameter(description = "分页参数",name = "pageResultDTO",required = true)PageResultDTO pageResultDTO){
        return villageService.villageList(pageResultDTO);
    }

    /**
     * 新增农村
     * @param villageBaseDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增农村")
    public Result addVillage(@RequestBody VillageBaseDTO villageBaseDTO){
        LocalDate now = LocalDate.now();
        stringRedisTemplate.opsForHyperLogLog().add("new:village"+now.format(DATE_FORMAT),villageBaseDTO.getName());
        return villageService.addVillage(villageBaseDTO);
    }

    /**
     * 修改农村信息
     * @param id
     * @param villageBaseDTO
     * @return
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改农村信息")
    public Result updateVillage(
            @Parameter(description = "农村id",name = "id",required = true,in = ParameterIn.PATH) @PathVariable Long id,
            @RequestBody VillageBaseDTO villageBaseDTO){
        return villageService.updateVillage(id, villageBaseDTO);
    }

    /**
     * 删除农村
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除农村")
    public Result deleteVillage(
            @Parameter(description = "农村id",name = "id",required = true,in = ParameterIn.PATH) @PathVariable Long id){
        boolean res = villageService.removeById(id);
        if(!res){
            return Result.error(ErrorConstants.ERROR_DELETE);
        }
        return Result.ok();
    }

    /**
     * 查看优质农村信息（top10，根据下属景点总点赞量）
     * @return
     */
    @GetMapping("/likes")
    @Operation(summary = "查看优质农村信息（top10，根据下属景点总点赞量）")
    public Result<List<VillageBaseVO>> villageLikes(){
        return villageService.villageLikes();
    }

    /**
     * 查看优质农村信息（top10，根据下属景点总收藏量）
     * @return
     */
    @GetMapping("/collections")
    @Operation(summary = "查看优质农村信息（top10，根据下属景点总收藏量）")
    public Result<List<VillageBaseVO>> villageCollections(){
        return villageService.villageCollections();
    }
}
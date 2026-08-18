package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.ScenicDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserCommentVO;
import com.shixiaoyu.xiangyueproject.service.ScenicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * 景点接口（用户端 + 管理端）
 * 管理端方法使用绝对路径 /admin/scenic/**，由 LoginInterceptor 校验 role=4
 */
@Slf4j
@RestController
@RequestMapping("/sc")
@Tag(name = "景点接口")
@RequiredArgsConstructor
public class ScenicController {
    private final ScenicService scenicService;

    @PostMapping("/rg")
    @Operation(summary = "农户/村长在所属村直接新增景点")
    public Result<Void> register(@Parameter(description = "景点信息", name = "scenicDTO", required = true)
                                 @Valid @RequestBody ScenicDTO scenicDTO) {
        scenicService.register(scenicDTO);
        return Result.ok();
    }

    @GetMapping("/top10")
    @Operation(summary = "优质景点 top10（按点赞量）")
    public Result<List<ScenicVO>> top10() {
        return Result.ok(scenicService.getTop10Scenic());
    }

    @GetMapping("/detail")
    @Operation(summary = "景点详情")
    public Result<ScenicVO> detail(@Parameter(description = "景点id", name = "id", required = true) @RequestParam Long id) {
        return Result.ok(scenicService.detail(id));
    }

    @GetMapping("/sccomments")
    @Operation(summary = "景点评论列表")
    public Result<List<UserCommentVO>> scComments(@Parameter(description = "景点id", name = "id", required = true) @RequestParam Long id) {
        return Result.ok(scenicService.getScComments(id));
    }

    // ==================== 管理端（LoginInterceptor 校验 role=4） ====================

    @GetMapping("/admin/scenic/list")
    @Operation(summary = "管理端：分页查询全部景点")
    public Result<PageResultVO<ScenicVO>> adminList(PageResultDTO pageResultDTO) {
        return Result.ok(scenicService.adminList(pageResultDTO));
    }

    @PostMapping("/admin/scenic")
    @Operation(summary = "管理端：直接新增景点到指定村落")
    public Result<Void> adminAdd(
            @Parameter(description = "农村id", name = "village_id", required = true, in = ParameterIn.QUERY) @RequestParam Long villageId,
            @Parameter(description = "景点信息", name = "scenicDTO", required = true) @RequestBody ScenicDTO scenicDTO) {
        scenicService.adminAdd(villageId, scenicDTO);
        return Result.ok();
    }

    @PostMapping("/admin/scenic/modify/{id}")
    @Operation(summary = "管理端：修改景点")
    public Result<Void> adminUpdate(
            @Parameter(description = "景点id", name = "id", required = true, in = ParameterIn.PATH) @PathVariable Long id,
            @Parameter(description = "景点信息", name = "scenicDTO", required = true) @RequestBody ScenicDTO scenicDTO) {
        scenicService.adminUpdate(id, scenicDTO);
        return Result.ok();
    }

    @PostMapping("/admin/scenic/del/{id}")
    @Operation(summary = "管理端：删除景点")
    public Result<Void> adminDelete(
            @Parameter(description = "景点id", name = "id", required = true, in = ParameterIn.PATH) @PathVariable Long id) {
        scenicService.adminDelete(id);
        return Result.ok();
    }
}

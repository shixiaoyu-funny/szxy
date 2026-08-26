package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.FarmerAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerAccessVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.enums.AccessStatusEnum;
import com.shixiaoyu.xiangyueproject.service.FarmerAccessService;
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

/**
 * 农户准入申请（游客申请 + 管理端审批）
 */
@Slf4j
@RestController
@RequestMapping("/fmr/access")
@Tag(name = "农户准入申请")
@RequiredArgsConstructor
public class FarmerAccessController {

    private final FarmerAccessService farmerAccessService;

    @PostMapping("/apply")
    @Operation(summary = "游客：提交/改提成为农户申请")
    public Result<Void> apply(@RequestBody FarmerAccessDTO dto) {
        farmerAccessService.apply(dto);
        return Result.ok();
    }

    @GetMapping("/mine")
    @Operation(summary = "查询本人最新申请")
    public Result<FarmerAccessVO> mine() {
        return Result.ok(farmerAccessService.mine());
    }

    @GetMapping("/list")
    @Operation(summary = "管理端：申请分页列表")
    public Result<PageResultVO<FarmerAccessVO>> list(
            PageResultDTO pageResultDTO,
            @Parameter(description = "状态：0待审 1已通过 2已拒绝") @RequestParam(required = false) Integer status) {
        return Result.ok(farmerAccessService.list(pageResultDTO, AccessStatusEnum.fromCode(status)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "管理端：通过申请")
    public Result<Void> approve(@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        farmerAccessService.approve(id);
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "管理端：拒绝申请")
    public Result<Void> reject(@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        farmerAccessService.reject(id);
        return Result.ok();
    }
}

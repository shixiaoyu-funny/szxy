package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VgHeadAccessVO;
import com.shixiaoyu.xiangyueproject.enums.VgHeadStatusEnum;
import com.shixiaoyu.xiangyueproject.service.VgHeadAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 村长准入申请（农户申请 + 村长初审 + 管理端终审）
 */
@Slf4j
@RestController
@RequestMapping("/fmr/vghead")
@Tag(name = "村长准入申请")
@RequiredArgsConstructor
public class VgHeadAccessController {

    private final VgHeadAccessService vgHeadAccessService;

    @PostMapping("/apply")
    @Operation(summary = "农户：申请成为本村村长")
    public Result<Void> apply() {
        vgHeadAccessService.apply();
        return Result.ok();
    }

    @GetMapping("/mine")
    @Operation(summary = "查询本人最新申请")
    public Result<VgHeadAccessVO> mine() {
        return Result.ok(vgHeadAccessService.mine());
    }

    @GetMapping("/my-list")
    @Operation(summary = "本人全部申请（消息页）")
    public Result<List<VgHeadAccessVO>> myList() {
        return Result.ok(vgHeadAccessService.myList());
    }

    @GetMapping("/pending-chief")
    @Operation(summary = "村长：本村待审列表")
    public Result<List<VgHeadAccessVO>> pendingChief() {
        return Result.ok(vgHeadAccessService.pendingForChief());
    }

    @GetMapping("/pending-chief/count")
    @Operation(summary = "村长：本村待审数量")
    public Result<Integer> pendingChiefCount() {
        return Result.ok(vgHeadAccessService.pendingChiefCount());
    }

    @GetMapping("/list")
    @Operation(summary = "管理端：申请分页列表")
    public Result<PageResultVO<VgHeadAccessVO>> list(
            PageResultDTO pageResultDTO,
            @Parameter(description = "状态：0待审 1村长已审 2管理员已审 3已拒绝")
            @RequestParam(required = false) Integer status) {
        return Result.ok(vgHeadAccessService.adminList(pageResultDTO, VgHeadStatusEnum.fromCode(status)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "申请详情")
    public Result<VgHeadAccessVO> detail(@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        return Result.ok(vgHeadAccessService.detail(id));
    }

    @PostMapping("/{id}/chief-approve")
    @Operation(summary = "村长：通过申请")
    public Result<Void> chiefApprove(@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        vgHeadAccessService.chiefApprove(id);
        return Result.ok();
    }

    @PostMapping("/{id}/chief-reject")
    @Operation(summary = "村长：拒绝申请")
    public Result<Void> chiefReject(@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        vgHeadAccessService.chiefReject(id);
        return Result.ok();
    }

    @PostMapping("/{id}/admin-approve")
    @Operation(summary = "管理端：终审通过")
    public Result<Void> adminApprove(@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        vgHeadAccessService.adminApprove(id);
        return Result.ok();
    }

    @PostMapping("/{id}/admin-reject")
    @Operation(summary = "管理端：拒绝")
    public Result<Void> adminReject(@Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        vgHeadAccessService.adminReject(id);
        return Result.ok();
    }
}

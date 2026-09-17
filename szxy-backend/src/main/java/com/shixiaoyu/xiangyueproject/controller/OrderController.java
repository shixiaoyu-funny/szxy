package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.OrderAlterDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderRefundApplyDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderRefundHandleDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderSubmitDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.OrderPreviewVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.TradeOrderVO;
import com.shixiaoyu.xiangyueproject.enums.OrderStatusEnum;
import com.shixiaoyu.xiangyueproject.service.TradeOrderService;
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
 * 订单接口（Controller 只转发，业务在 TradeOrderService）
 * <p>
 * 支付不在这里：统一走 {@code POST /wallet/pay/{orderId}}，避免两套扣款逻辑。
 * 下单同步落库；仅「15 分钟未支付取消」使用延迟队列。
 */
@RestController
@RequestMapping("/order")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "订单")
public class OrderController {

    private final TradeOrderService tradeOrderService;

    // ---------- 查询 ----------

    /** 买家历史单；可按 status 筛选；结果带明细 */
    @GetMapping("/ls")
    @Operation(summary = "买家历史订单分页")
    public Result<PageResultVO<TradeOrderVO>> ls(
            PageResultDTO pageResultDTO,
            @Parameter(description = "订单状态筛选，可选") @RequestParam(required = false) Integer status) {
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(status);
        return Result.ok(tradeOrderService.ls(pageResultDTO, orderStatusEnum));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<TradeOrderVO> detail(
            @Parameter(description = "订单ID", required = true) @PathVariable Long id) {
        return Result.ok(tradeOrderService.detail(id));
    }

    // ---------- 下单（同步） ----------

    /** 确认页：算金额/余额缺口，不写库 */
    @PostMapping("/preview")
    @Operation(summary = "确认页预览（不算单）")
    public Result<OrderPreviewVO> preview(@RequestBody OrderSubmitDTO dto) {
        return Result.ok(tradeOrderService.preview(dto));
    }

    /**
     * 生成待支付单：fromCart 或 立即购买；
     * 成功后投递 15min 延迟消息，超时未付自动取消
     */
    @PostMapping("/add")
    @Operation(summary = "生成待支付订单（同步，15分钟未支付自动取消）")
    public Result<TradeOrderVO> add(@RequestBody OrderSubmitDTO dto) {
        return Result.ok(tradeOrderService.create(dto));
    }

    /** 仅待支付可改地址/备注 */
    @PostMapping("/alter")
    @Operation(summary = "修改待支付订单地址/备注")
    public Result<Void> alter(@RequestBody OrderAlterDTO dto) {
        tradeOrderService.alterUnpaid(dto);
        return Result.ok();
    }

    // ---------- 买家状态流转 ----------

    /** 待支付 → 已取消（未扣库存，无需回补） */
    @PostMapping("/cancel/{id}")
    @Operation(summary = "取消订单（待支付→已取消）")
    public Result<Void> cancel(@PathVariable Long id) {
        tradeOrderService.cancel(id);
        return Result.ok();
    }

    /** 可退状态 → 退款中，并记录申请前状态供驳回恢复 */
    @PostMapping("/refund/{id}")
    @Operation(summary = "申请退款")
    public Result<Void> refund(@PathVariable Long id, @RequestBody OrderRefundApplyDTO dto) {
        tradeOrderService.applyRefund(id, dto);
        return Result.ok();
    }

    /** 待签收 → 已签收 */
    @PostMapping("/confirm/{id}")
    @Operation(summary = "确认收货（待签收→已签收）")
    public Result<Void> confirm(@PathVariable Long id) {
        tradeOrderService.confirmReceipt(id);
        return Result.ok();
    }

    /** 虚拟：待使用 → 已使用，明细置已核销 */
    @PostMapping("/use/{id}")
    @Operation(summary = "使用/核销虚拟订单（待使用→已使用）")
    public Result<Void> use(@PathVariable Long id) {
        tradeOrderService.useOrder(id);
        return Result.ok();
    }

    /** 终态软删：已取消/已签收/已使用/已退款 */
    @PostMapping("/del/{id}")
    @Operation(summary = "软删除订单（终态）")
    public Result<Void> del(@PathVariable Long id) {
        tradeOrderService.softDelete(id);
        return Result.ok();
    }

    // ---------- 卖家 ----------

    /** 按 order_item.seller_user_id 反查 */
    @GetMapping("/seller/ls")
    @Operation(summary = "卖家订单分页")
    public Result<PageResultVO<TradeOrderVO>> sellerLs(
            PageResultDTO pageResultDTO,
            @RequestParam(required = false) Integer status) {
        return Result.ok(tradeOrderService.sellerLs(pageResultDTO, OrderStatusEnum.fromCode(status)));
    }

    /** 待发货 → 待收货 */
    @PostMapping("/seller/ship/{id}")
    @Operation(summary = "卖家发货（待发货→待收货）")
    public Result<Void> sellerShip(@PathVariable Long id) {
        tradeOrderService.sellerShip(id);
        return Result.ok();
    }

    /** handle=1 同意（入账+回库存）；=2 驳回（恢复申请前状态） */
    @PostMapping("/seller/refund/{id}")
    @Operation(summary = "卖家处理退款（同意入账回库存 / 驳回恢复原状态）")
    public Result<Void> sellerRefund(@PathVariable Long id, @RequestBody OrderRefundHandleDTO dto) {
        tradeOrderService.sellerHandleRefund(id, dto);
        return Result.ok();
    }

    // ---------- 管理员模拟物流 ----------

    /** MVP：无真实物流，管理员推进 待收货 → 待签收 */
    @PostMapping("/admin/deliver/{id}")
    @Operation(summary = "管理员模拟送达（待收货→待签收）")
    public Result<Void> adminDeliver(@PathVariable Long id) {
        tradeOrderService.adminDeliver(id);
        return Result.ok();
    }
}

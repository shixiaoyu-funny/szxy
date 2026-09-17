package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.RechargeDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.WalletRecordsSplitVO;
import com.shixiaoyu.xiangyueproject.service.WalletRecordService;
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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 充值与支付（写操作集中在此，与 OrderController 解耦）
 * <p>
 * 支付走乐观锁：CAS 订单状态 + 扣库存 + 扣余额，同一事务，防重复支付与超卖。
 */
@Slf4j
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Tag(name = "充值与支付")
public class RechargeAndPaymentController {

    private final WalletRecordService walletRecordService;

    /** 展示用余额；扣款以 pay 接口内校验为准 */
    @GetMapping("/balance")
    @Operation(summary = "查询当前用户余额")
    public Result<BigDecimal> balance() {
        return Result.ok(walletRecordService.balance());
    }

    /** 双分页：recharge=充值；consume=支付+退款 */
    @GetMapping("/records")
    @Operation(summary = "钱包流水双分页（充值 + 消费）")
    public Result<WalletRecordsSplitVO> records(PageResultDTO pageResultDTO) {
        return Result.ok(walletRecordService.recordsSplit(pageResultDTO));
    }

    /** MVP 模拟充值，直接加余额并记流水 */
    @PostMapping("/recharge")
    @Operation(summary = "模拟充值")
    public Result<Void> recharge(@RequestBody RechargeDTO dto) {
        walletRecordService.recharge(dto);
        return Result.ok();
    }

    /**
     * 余额支付订单：
     * CAS 待支付→待使用/待发货 → 扣库存 → 扣余额 → 写流水（虚拟单发核销码）
     */
    @PostMapping("/pay/{orderId}")
    @Operation(summary = "余额支付订单")
    public Result<Void> pay(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {
        walletRecordService.payOrder(orderId);
        return Result.ok();
    }
}

package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.RechargeDTO;
import com.shixiaoyu.xiangyueproject.entity.po.TradeOrder;
import com.shixiaoyu.xiangyueproject.entity.po.WalletRecord;
import com.shixiaoyu.xiangyueproject.entity.vo.WalletRecordsSplitVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包：余额/流水查询 + 充值 + 订单支付（乐观锁）+ 退款入账
 */
public interface WalletRecordService extends IService<WalletRecord> {
    BigDecimal balance();

    WalletRecordsSplitVO recordsSplit(PageResultDTO pageResultDTO);

    /** 模拟充值入账 */
    void recharge(RechargeDTO dto);

    /**
     * 余额支付订单：CAS 改订单状态 + 扣库存 + 扣余额 + 写流水（同一事务）
     */
    void payOrder(Long orderId);

    /**
     * 卖家同意退款后入账（调用方已 CAS 订单为已退款）
     */
    void refundCredit(TradeOrder order, List<com.shixiaoyu.xiangyueproject.entity.po.OrderItem> items);
}

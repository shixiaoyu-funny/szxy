package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderAlterDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderRefundApplyDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderRefundHandleDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderSubmitDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.po.TradeOrder;
import com.shixiaoyu.xiangyueproject.entity.vo.OrderPreviewVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.TradeOrderVO;
import com.shixiaoyu.xiangyueproject.enums.OrderStatusEnum;

/**
 * 交易订单：查询 / 下单 / 状态流转（支付走 WalletRecordService）
 */
public interface TradeOrderService extends IService<TradeOrder> {
    PageResultVO<TradeOrderVO> ls(PageResultDTO pageResultDTO, OrderStatusEnum status);

    TradeOrderVO detail(Long id);

    void softDelete(Long id);

    OrderPreviewVO preview(OrderSubmitDTO dto);

    /** 同步生成待支付订单（不走 MQ），并投递 15 分钟支付超时延迟消息 */
    TradeOrderVO create(OrderSubmitDTO dto);

    void alterUnpaid(OrderAlterDTO dto);

    void cancel(Long id);

    /** 延迟队列到期：若仍待支付则取消（无登录态） */
    void cancelUnpaidTimeout(Long orderId);

    void applyRefund(Long id, OrderRefundApplyDTO dto);

    void confirmReceipt(Long id);

    void useOrder(Long id);

    PageResultVO<TradeOrderVO> sellerLs(PageResultDTO pageResultDTO, OrderStatusEnum status);

    void sellerShip(Long id);

    void sellerHandleRefund(Long id, OrderRefundHandleDTO dto);

    /** 管理员模拟送达：待收货 → 待签收 */
    void adminDeliver(Long id);
}

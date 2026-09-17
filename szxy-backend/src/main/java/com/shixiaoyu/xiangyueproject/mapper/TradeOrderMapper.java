package com.shixiaoyu.xiangyueproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shixiaoyu.xiangyueproject.entity.po.TradeOrder;
import com.shixiaoyu.xiangyueproject.enums.OrderStatusEnum;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 交易订单 mapper（含状态 CAS，支付幂等）
 */
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    /**
     * 乐观改状态：仅当当前 status=expect 时成功（支付/取消幂等关键）
     */
    @Update("UPDATE trade_order SET status = #{newStatus}, update_time = NOW() " +
            "WHERE id = #{id} AND status = #{expectStatus} AND is_deleted = 0")
    int casUpdateStatus(@Param("id") Long id,
                        @Param("expectStatus") OrderStatusEnum expectStatus,
                        @Param("newStatus") OrderStatusEnum newStatus);

    /**
     * 支付成功：待支付 → 待使用/待发货，并写 pay_time
     */
    @Update("UPDATE trade_order SET status = #{newStatus}, pay_time = #{payTime}, update_time = NOW() " +
            "WHERE id = #{id} AND status = 1 AND is_deleted = 0")
    int casPaySuccess(@Param("id") Long id,
                      @Param("newStatus") OrderStatusEnum newStatus,
                      @Param("payTime") LocalDateTime payTime);
}

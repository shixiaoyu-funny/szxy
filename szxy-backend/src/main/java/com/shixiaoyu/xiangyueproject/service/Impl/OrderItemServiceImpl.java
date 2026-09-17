package com.shixiaoyu.xiangyueproject.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.po.OrderItem;
import com.shixiaoyu.xiangyueproject.mapper.OrderItemMapper;
import com.shixiaoyu.xiangyueproject.service.OrderItemService;
import org.springframework.stereotype.Service;

/**
 * 订单明细服务实现
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem>
        implements OrderItemService {
}

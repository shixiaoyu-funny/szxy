package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.RechargeDTO;
import com.shixiaoyu.xiangyueproject.entity.po.OrderItem;
import com.shixiaoyu.xiangyueproject.entity.po.TradeOrder;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.WalletRecord;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.WalletRecordVO;
import com.shixiaoyu.xiangyueproject.entity.vo.WalletRecordsSplitVO;
import com.shixiaoyu.xiangyueproject.enums.FulfillmentTypeEnum;
import com.shixiaoyu.xiangyueproject.enums.OrderStatusEnum;
import com.shixiaoyu.xiangyueproject.enums.VerifyStatusEnum;
import com.shixiaoyu.xiangyueproject.enums.WalletRecordTypeEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.OrderItemMapper;
import com.shixiaoyu.xiangyueproject.mapper.ShopProductMapper;
import com.shixiaoyu.xiangyueproject.mapper.TradeOrderMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.WalletRecordMapper;
import com.shixiaoyu.xiangyueproject.service.WalletRecordService;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 钱包业务实现
 * <p>
 * - 查询：余额、充值/消费双分页流水（仅展示）
 * - 写操作：充值、支付、退款入账，均在事务内完成
 * <p>
 * 支付乐观锁约定（防重复支付 + 防超卖）：
 * 1. {@code UPDATE trade_order ... WHERE status=待支付} 影响行数必须为 1
 * 2. {@code UPDATE shop_product SET stock=stock-n WHERE stock>=n} 每行必须成功
 * 3. {@code UPDATE user SET balance=balance-x WHERE balance>=x} 必须成功
 * 任一步失败 → 事务回滚，订单仍为待支付（或回到支付前）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletRecordServiceImpl extends ServiceImpl<WalletRecordMapper, WalletRecord>
        implements WalletRecordService {

    private final UserMapper userMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShopProductMapper shopProductMapper;

    /**
     * 读 user.balance；空当 0。仅 UX，真正扣款以 payOrder 内 CAS 为准
     */
    @Override
    public BigDecimal balance() {
        Long userId = UserHolder.getUser().getId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
    }

    /**
     * 个人中心流水：两侧分页参数相同
     * - recharge：充值
     * - consume：支付扣款 + 退款入账
     */
    @Override
    public WalletRecordsSplitVO recordsSplit(PageResultDTO pageResultDTO) {
        Long userId = UserHolder.getUser().getId();
        int pageNo = pageResultDTO != null && pageResultDTO.getPageNo() != null && pageResultDTO.getPageNo() > 0
                ? pageResultDTO.getPageNo() : 1;
        int pageSize = pageResultDTO != null && pageResultDTO.getPageSize() != null && pageResultDTO.getPageSize() > 0
                ? pageResultDTO.getPageSize() : 10;

        PageResultVO<WalletRecordVO> recharge = pageByTypes(
                userId, pageNo, pageSize, List.of(WalletRecordTypeEnum.RECHARGE));
        PageResultVO<WalletRecordVO> consume = pageByTypes(
                userId, pageNo, pageSize, List.of(WalletRecordTypeEnum.PAY, WalletRecordTypeEnum.REFUND));
        return new WalletRecordsSplitVO(recharge, consume);
    }

    /**
     * 模拟充值（不接微信/支付宝）：加余额 + 写 RECHARGE 流水，balance_after 记变动后余额
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(RechargeDTO dto) {
        if (dto == null || dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额须大于 0");
        }
        Long userId = UserHolder.getUser().getId();
        int rows = userMapper.addBalance(userId, dto.getAmount());
        if (rows != 1) {
            throw new BusinessException("充值失败");
        }
        User user = userMapper.selectById(userId);
        WalletRecord record = new WalletRecord();
        record.setUserId(userId);
        record.setType(WalletRecordTypeEnum.RECHARGE);
        record.setAmount(dto.getAmount());
        record.setBalanceAfter(user.getBalance());
        record.setRemark("模拟充值");
        save(record);
    }

    /**
     * 余额支付核心（同一事务，顺序不可随意调换）：
     * <ol>
     *   <li>校验本人待支付订单</li>
     *   <li>按履约类型决定支付后状态：虚拟→待使用，实物→待发货</li>
     *   <li>CAS 改订单状态（并发二次支付 / 超时取消后支付 → 影响行数 0，拒绝）</li>
     *   <li>按明细 CAS 扣库存（stock&gt;=qty），失败回滚 → 防超卖</li>
     *   <li>CAS 扣余额（balance&gt;=amount），失败回滚</li>
     *   <li>写 PAY 流水；虚拟单生成核销码</li>
     * </ol>
     * 下单时投递的 15 分钟延迟消息到期后，若本单已非待支付，超时消费者会忽略。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        Long userId = UserHolder.getUser().getId();
        TradeOrder order = tradeOrderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId()) || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != OrderStatusEnum.UNPAID) {
            throw new BusinessException("订单不是待支付状态，无法支付");
        }

        // 支付后进入哪条履约轨道
        OrderStatusEnum afterPay = order.getFulfillmentType() == FulfillmentTypeEnum.VIRTUAL
                ? OrderStatusEnum.TO_USE
                : OrderStatusEnum.TO_SHIP;
        LocalDateTime payTime = LocalDateTime.now();

        // ① 状态乐观锁：WHERE status=待支付，抢到唯一「支付权」
        int cas = tradeOrderMapper.casPaySuccess(orderId, afterPay, payTime);
        if (cas != 1) {
            throw new BusinessException("订单状态已变更，请勿重复支付");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            throw new BusinessException("订单明细为空");
        }

        // ② 库存乐观锁：WHERE stock>=qty，任一行失败则整单回滚
        for (OrderItem item : items) {
            int stockRows = shopProductMapper.deductStock(item.getProductId(), item.getQuantity());
            if (stockRows != 1) {
                throw new BusinessException("库存不足：" + item.getItemName());
            }
        }

        // ③ 余额乐观锁：WHERE balance>=amount
        BigDecimal payAmount = order.getPayAmount();
        int balRows = userMapper.deductBalance(userId, payAmount);
        if (balRows != 1) {
            throw new BusinessException("余额不足，请先充值");
        }

        // ④ 流水：amount 记正数，方向由 type=PAY 表达
        User user = userMapper.selectById(userId);
        WalletRecord record = new WalletRecord();
        record.setUserId(userId);
        record.setType(WalletRecordTypeEnum.PAY);
        record.setAmount(payAmount);
        record.setBalanceAfter(user.getBalance());
        record.setOrderId(orderId);
        record.setRemark("订单支付 " + order.getOrderNo());
        save(record);

        // ⑤ 虚拟履约：支付成功才发核销码（取消/未支付不会有码）
        if (order.getFulfillmentType() == FulfillmentTypeEnum.VIRTUAL) {
            for (OrderItem item : items) {
                item.setVerifyCode(IdUtil.simpleUUID().substring(0, 16).toUpperCase());
                item.setVerifyStatus(VerifyStatusEnum.UNVERIFIED);
                orderItemMapper.updateById(item);
            }
        }
        log.info("订单支付成功 orderId={}, userId={}, amount={}", orderId, userId, payAmount);
    }

    /**
     * 卖家同意退款后调用：退金额入买家余额 + 按明细回补库存 + 写 REFUND 流水
     * 调用方须已把订单 CAS 到「已退款」，避免重复入账
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundCredit(TradeOrder order, List<OrderItem> items) {
        Long userId = order.getUserId();
        BigDecimal amount = order.getPayAmount();
        int balRows = userMapper.addBalance(userId, amount);
        if (balRows != 1) {
            throw new BusinessException("退款入账失败");
        }
        // 支付时扣过库存，退款对称回补
        for (OrderItem item : items) {
            shopProductMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
        User user = userMapper.selectById(userId);
        WalletRecord record = new WalletRecord();
        record.setUserId(userId);
        record.setType(WalletRecordTypeEnum.REFUND);
        record.setAmount(amount);
        record.setBalanceAfter(user.getBalance());
        record.setOrderId(order.getId());
        record.setRemark("订单退款 " + order.getOrderNo());
        save(record);
    }

    /**
     * 按流水类型集合分页
     */
    private PageResultVO<WalletRecordVO> pageByTypes(
            Long userId, int pageNo, int pageSize, List<WalletRecordTypeEnum> types) {
        Page<WalletRecord> page = lambdaQuery()
                .eq(WalletRecord::getUserId, userId)
                .in(WalletRecord::getType, types)
                .orderByDesc(WalletRecord::getCreateTime)
                .page(Page.of(pageNo, pageSize));
        List<WalletRecordVO> vos = page.getRecords().stream()
                .map(r -> BeanUtil.copyProperties(r, WalletRecordVO.class))
                .toList();
        return new PageResultVO<>(page.getTotal(), vos);
    }
}

package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderAlterDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderRefundApplyDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderRefundHandleDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.OrderSubmitDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.po.CartItem;
import com.shixiaoyu.xiangyueproject.entity.po.OrderItem;
import com.shixiaoyu.xiangyueproject.entity.po.ShopProduct;
import com.shixiaoyu.xiangyueproject.entity.po.TradeOrder;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.UserAddress;
import com.shixiaoyu.xiangyueproject.entity.vo.OrderPreviewVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.TradeOrderVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserAddressVO;
import com.shixiaoyu.xiangyueproject.enums.FulfillmentTypeEnum;
import com.shixiaoyu.xiangyueproject.enums.OrderStatusEnum;
import com.shixiaoyu.xiangyueproject.enums.RefundHandleEnum;
import com.shixiaoyu.xiangyueproject.enums.VerifyStatusEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.CartItemMapper;
import com.shixiaoyu.xiangyueproject.mapper.OrderItemMapper;
import com.shixiaoyu.xiangyueproject.mapper.ShopProductMapper;
import com.shixiaoyu.xiangyueproject.mapper.TradeOrderMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserAddressMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.service.TradeOrderService;
import com.shixiaoyu.xiangyueproject.service.WalletRecordService;
import com.shixiaoyu.xiangyueproject.utils.SecurityUtil;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.ORDER_PAY_DELAY_EXCHANGE;
import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.ORDER_PAY_DELAY_ROUTING_KEY;

/**
 * 交易订单业务实现
 * <p>
 * 职责边界：
 * - 本类：下单/预览/改单、状态流转（取消/退款/发货/签收/使用）、超时取消
 * - 支付扣款与扣库存：见 {@link WalletRecordService#payOrder}（乐观锁 CAS）
 * <p>
 * 双轨状态机（支付后按履约类型分支）：
 * - 虚拟：待支付 → 待使用 → 已使用（或中途退款）
 * - 实物：待支付 → 待发货 → 待收货 → 待签收 → 已签收（或中途退款）
 * <p>
 * 库存策略：下单只读校验库存，真正扣减在支付 CAS；超时/手动取消待支付单无需回补库存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeOrderServiceImpl extends ServiceImpl<TradeOrderMapper, TradeOrder>
        implements TradeOrderService {

    /** 买家可软删的终态（只改 is_deleted，不改 status） */
    private static final Set<OrderStatusEnum> DELETABLE = EnumSet.of(
            OrderStatusEnum.CANCELLED,
            OrderStatusEnum.SIGNED,
            OrderStatusEnum.USED,
            OrderStatusEnum.REFUNDED
    );

    /**
     * 可申请退款的状态：
     * 虚拟=待使用；实物=待发货/待收货/待签收/已签收
     */
    private static final Set<OrderStatusEnum> REFUNDABLE = EnumSet.of(
            OrderStatusEnum.TO_USE,
            OrderStatusEnum.TO_SHIP,
            OrderStatusEnum.TO_RECEIVE,
            OrderStatusEnum.TO_SIGN,
            OrderStatusEnum.SIGNED
    );

    private final OrderItemMapper orderItemMapper;
    private final ShopProductMapper shopProductMapper;
    private final CartItemMapper cartItemMapper;
    private final UserAddressMapper userAddressMapper;
    private final UserMapper userMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final WalletRecordService walletRecordService;
    private final RabbitTemplate rabbitTemplate;

    // ======================== 查询 / 软删 ========================

    /** 买家订单分页：排除软删，可选按 status 筛选，明细一并带出 */
    @Override
    public PageResultVO<TradeOrderVO> ls(PageResultDTO pageResultDTO, OrderStatusEnum status) {
        Long userId = UserHolder.getUser().getId();
        Page<TradeOrder> page = lambdaQuery()
                .eq(TradeOrder::getUserId, userId)
                .eq(TradeOrder::getIsDeleted, 0)
                .eq(status != null, TradeOrder::getStatus, status)
                .orderByDesc(TradeOrder::getCreateTime)
                .page(Page.of(pageNo(pageResultDTO), pageSize(pageResultDTO)));
        return toPageVo(page);
    }

    /** 买家订单详情（含明细） */
    @Override
    public TradeOrderVO detail(Long id) {
        return toVo(requireOwnOrder(id), loadItems(id));
    }

    /**
     * 软删除：仅终态可删；列表/详情以 is_deleted=0 过滤，status 保持终态原值便于统计
     */
    @Override
    public void softDelete(Long id) {
        TradeOrder order = requireOwnOrder(id);
        if (!DELETABLE.contains(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许删除");
        }
        order.setIsDeleted(1);
        updateById(order);
    }

    // ======================== 预览 / 下单 ========================

    /**
     * 确认页预览（不落库）：算总额、余额、缺口、是否需要地址、明细清单
     * shortage &gt; 0 时前端可引导去充值
     */
    @Override
    public OrderPreviewVO preview(OrderSubmitDTO dto) {
        Long userId = UserHolder.getUser().getId();
        List<Line> lines = resolveLines(dto, userId);
        FulfillmentTypeEnum fulfillment = requireSameFulfillment(lines);

        BigDecimal total = lines.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        User user = userMapper.selectById(userId);
        BigDecimal balance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        // 余额够则缺口为 0，不够则为差额
        BigDecimal shortage = total.subtract(balance).max(BigDecimal.ZERO);

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setBalance(balance);
        vo.setTotalAmount(total);
        vo.setShortage(shortage);
        vo.setFulfillmentType(fulfillment);
        vo.setNeedAddress(fulfillment == FulfillmentTypeEnum.PHYSICAL);
        vo.setItems(lines.stream().map(l -> {
            OrderPreviewVO.Item item = new OrderPreviewVO.Item();
            item.setProductId(l.product.getId());
            item.setProductName(l.product.getName());
            item.setProductImage(l.product.getImage());
            item.setUnitPrice(l.product.getPrice());
            item.setQuantity(l.quantity);
            item.setSubtotal(l.subtotal());
            item.setStock(l.product.getStock());
            item.setFulfillmentType(l.fulfillment);
            return item;
        }).toList());

        // 前端若已选地址，预览里带回便于确认页展示
        if (dto.getAddressId() != null) {
            UserAddress addr = requireOwnAddress(dto.getAddressId(), userId);
            vo.setAddress(BeanUtil.copyProperties(addr, UserAddressVO.class));
        }
        return vo;
    }

    /**
     * 同步生成待支付订单（下单不走 MQ）。
     * <p>
     * 步骤：
     * 1. 解析商品行（购物车勾选 / 立即购买），校验不可虚拟+实物混单
     * 2. 实物必须有收货地址，并写入地址快照（防用户事后改地址影响历史单）
     * 3. 写 trade_order + order_item（商品名/价快照）；购物车结算则清掉对应勾选项
     * 4. 投递「支付超时」延迟消息（TTL 15min → 死信取消队列）；到期仍待支付则自动取消
     * <p>
     * 注意：此处不扣库存；库存在支付时 CAS 扣减，避免占库存后不付款
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrderVO create(OrderSubmitDTO dto) {
        Long userId = UserHolder.getUser().getId();
        List<Line> lines = resolveLines(dto, userId);
        FulfillmentTypeEnum fulfillment = requireSameFulfillment(lines);

        // 实物强制地址；虚拟可不填
        UserAddress address = null;
        String addressSnapshot = null;
        if (fulfillment == FulfillmentTypeEnum.PHYSICAL) {
            if (dto.getAddressId() == null) {
                throw new BusinessException("实物订单须选择收货地址");
            }
            address = requireOwnAddress(dto.getAddressId(), userId);
            // 快照：下单瞬间的收件信息，后续改地址簿不影响本单
            addressSnapshot = JSONUtil.toJsonStr(BeanUtil.copyProperties(address, UserAddressVO.class));
        }

        BigDecimal total = lines.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        TradeOrder order = new TradeOrder();
        order.setOrderNo(genOrderNo());
        order.setUserId(userId);
        order.setStatus(OrderStatusEnum.UNPAID);
        order.setFulfillmentType(fulfillment);
        order.setAddressId(address == null ? null : address.getId());
        order.setAddressSnapshot(addressSnapshot);
        order.setTotalAmount(total);
        order.setPayAmount(total); // MVP：实付=总额，无优惠券
        order.setRemark(dto.getRemark());
        order.setIsDeleted(0);
        save(order);

        // 明细快照：名称/封面/单价/卖家，避免商品改价后历史订单错乱
        List<OrderItem> items = new ArrayList<>();
        for (Line line : lines) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(line.product.getId());
            item.setScenicId(line.product.getScenicId());
            item.setSellerUserId(line.product.getUserId());
            item.setItemName(line.product.getName());
            item.setItemImage(line.product.getImage());
            item.setUnitPrice(line.product.getPrice());
            item.setQuantity(line.quantity);
            item.setSubtotal(line.subtotal());
            item.setFulfillmentType(line.fulfillment);
            item.setVerifyStatus(VerifyStatusEnum.UNVERIFIED);
            orderItemMapper.insert(item);
            items.add(item);
        }

        // 从购物车结算：下单成功后移除已下单商品，避免重复结算
        if (Boolean.TRUE.equals(dto.getFromCart())) {
            List<Long> productIds = lines.stream().map(l -> l.product.getId()).toList();
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .in(CartItem::getProductId, productIds));
        }

        // 延迟队列：消息在 delay 队列趴 15 分钟，过期进 cancel 队列由消费者处理
        Map<String, String> msg = new HashMap<>(2);
        msg.put("orderId", order.getId().toString());
        rabbitTemplate.convertAndSend(ORDER_PAY_DELAY_EXCHANGE, ORDER_PAY_DELAY_ROUTING_KEY, msg);
        log.info("创建待支付订单 orderId={}, orderNo={}, 已投递15分钟超时消息", order.getId(), order.getOrderNo());

        return toVo(order, items);
    }

    /**
     * 修改待支付订单：只允许改地址/备注，不能改商品与金额、不能改 status
     * 实物必须始终带有效地址；虚拟可选填地址
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void alterUnpaid(OrderAlterDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("订单ID不能为空");
        }
        TradeOrder order = requireOwnOrder(dto.getId());
        if (order.getStatus() != OrderStatusEnum.UNPAID) {
            throw new BusinessException("仅待支付订单可修改");
        }
        if (order.getFulfillmentType() == FulfillmentTypeEnum.PHYSICAL) {
            if (dto.getAddressId() == null) {
                throw new BusinessException("实物订单须保留收货地址");
            }
            UserAddress address = requireOwnAddress(dto.getAddressId(), order.getUserId());
            order.setAddressId(address.getId());
            order.setAddressSnapshot(JSONUtil.toJsonStr(BeanUtil.copyProperties(address, UserAddressVO.class)));
        } else if (dto.getAddressId() != null) {
            UserAddress address = requireOwnAddress(dto.getAddressId(), order.getUserId());
            order.setAddressId(address.getId());
            order.setAddressSnapshot(JSONUtil.toJsonStr(BeanUtil.copyProperties(address, UserAddressVO.class)));
        }
        if (dto.getRemark() != null) {
            order.setRemark(dto.getRemark());
        }
        updateById(order);
    }

    // ======================== 买家状态流转 ========================

    /**
     * 买家手动取消：CAS 待支付→已取消
     * 未扣库存故无需回补；延迟队列稍后到期时会因非待支付而自然忽略
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        TradeOrder order = requireOwnOrder(id);
        int rows = tradeOrderMapper.casUpdateStatus(id, OrderStatusEnum.UNPAID, OrderStatusEnum.CANCELLED);
        if (rows != 1) {
            throw new BusinessException("仅待支付订单可取消");
        }
        log.info("买家取消订单 orderId={}", id);
    }

    /**
     * 延迟队列到期自动取消（无登录态，由 MQ 消费者调用）
     * CAS 失败说明已支付/已取消 → 忽略即可，保证幂等
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelUnpaidTimeout(Long orderId) {
        if (orderId == null) {
            return;
        }
        int rows = tradeOrderMapper.casUpdateStatus(orderId, OrderStatusEnum.UNPAID, OrderStatusEnum.CANCELLED);
        if (rows == 1) {
            log.info("支付超时自动取消 orderId={}", orderId);
        } else {
            log.debug("支付超时忽略（已非待支付） orderId={}", orderId);
        }
    }

    /**
     * 申请退款：先把当前 status 记入 status_before_refund，再 CAS 进入退款中
     * 卖家驳回时靠 status_before_refund 原样恢复
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long id, OrderRefundApplyDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getReason())) {
            throw new BusinessException("请填写退款原因");
        }
        TradeOrder order = requireOwnOrder(id);
        if (!REFUNDABLE.contains(order.getStatus())) {
            throw new BusinessException("当前订单状态不可申请退款");
        }
        OrderStatusEnum before = order.getStatus();
        int rows = tradeOrderMapper.casUpdateStatus(id, before, OrderStatusEnum.REFUNDING);
        if (rows != 1) {
            throw new BusinessException("申请退款失败，请刷新后重试");
        }
        order.setStatusBeforeRefund(before);
        order.setRefundReason(dto.getReason());
        order.setRefundApplyTime(LocalDateTime.now());
        order.setStatus(OrderStatusEnum.REFUNDING);
        updateById(order);
    }

    /** 确认收货：待签收 → 已签收（实物终态） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long id) {
        TradeOrder order = requireOwnOrder(id);
        int rows = tradeOrderMapper.casUpdateStatus(id, OrderStatusEnum.TO_SIGN, OrderStatusEnum.SIGNED);
        if (rows != 1) {
            throw new BusinessException("仅待签收订单可确认收货");
        }
        order.setStatus(OrderStatusEnum.SIGNED);
        order.setFinishTime(LocalDateTime.now());
        updateById(order);
    }

    /**
     * 虚拟单「使用」：待使用 → 已使用，并把明细核销状态置为已核销
     * （MVP 由买家确认使用；若以后要卖家扫码核销可另开接口）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useOrder(Long id) {
        TradeOrder order = requireOwnOrder(id);
        if (order.getFulfillmentType() != FulfillmentTypeEnum.VIRTUAL) {
            throw new BusinessException("仅虚拟订单可核销使用");
        }
        int rows = tradeOrderMapper.casUpdateStatus(id, OrderStatusEnum.TO_USE, OrderStatusEnum.USED);
        if (rows != 1) {
            throw new BusinessException("仅待使用订单可确认使用");
        }
        List<OrderItem> items = loadItems(id);
        LocalDateTime now = LocalDateTime.now();
        for (OrderItem item : items) {
            item.setVerifyStatus(VerifyStatusEnum.VERIFIED);
            item.setVerifyTime(now);
            orderItemMapper.updateById(item);
        }
        order.setStatus(OrderStatusEnum.USED);
        order.setFinishTime(now);
        updateById(order);
    }

    // ======================== 卖家 / 管理员 ========================

    /**
     * 卖家订单列表：通过 order_item.seller_user_id 反查订单
     * （一单多卖家时，任一明细属于当前用户即可出现在列表）
     */
    @Override
    public PageResultVO<TradeOrderVO> sellerLs(PageResultDTO pageResultDTO, OrderStatusEnum status) {
        Long sellerId = UserHolder.getUser().getId();
        List<OrderItem> sellerItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getSellerUserId, sellerId));
        if (sellerItems.isEmpty()) {
            return new PageResultVO<>(0L, Collections.emptyList());
        }
        Set<Long> orderIds = sellerItems.stream().map(OrderItem::getOrderId).collect(Collectors.toSet());
        Page<TradeOrder> page = lambdaQuery()
                .in(TradeOrder::getId, orderIds)
                .eq(TradeOrder::getIsDeleted, 0)
                .eq(status != null, TradeOrder::getStatus, status)
                .orderByDesc(TradeOrder::getCreateTime)
                .page(Page.of(pageNo(pageResultDTO), pageSize(pageResultDTO)));
        return toPageVo(page);
    }

    /** 卖家发货：待发货 → 待收货，记录 ship_time */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sellerShip(Long id) {
        TradeOrder order = requireSellerOrder(id);
        int rows = tradeOrderMapper.casUpdateStatus(id, OrderStatusEnum.TO_SHIP, OrderStatusEnum.TO_RECEIVE);
        if (rows != 1) {
            throw new BusinessException("仅待发货订单可发货");
        }
        order.setStatus(OrderStatusEnum.TO_RECEIVE);
        order.setShipTime(LocalDateTime.now());
        updateById(order);
    }

    /**
     * 卖家处理退款：
     * - 同意：退款中 → 已退款，再调钱包入账 + 回补库存
     * - 驳回：退款中 → status_before_refund（缺失时按履约类型兜底回待使用/待发货）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sellerHandleRefund(Long id, OrderRefundHandleDTO dto) {
        if (dto == null || dto.getHandle() == null) {
            throw new BusinessException("请选择同意或驳回");
        }
        TradeOrder order = requireSellerOrder(id);
        if (order.getStatus() != OrderStatusEnum.REFUNDING) {
            throw new BusinessException("订单不在退款中");
        }
        List<OrderItem> items = loadItems(id);

        if (dto.getHandle() == RefundHandleEnum.APPROVE) {
            int rows = tradeOrderMapper.casUpdateStatus(id, OrderStatusEnum.REFUNDING, OrderStatusEnum.REFUNDED);
            if (rows != 1) {
                throw new BusinessException("处理退款失败，请刷新后重试");
            }
            order.setStatus(OrderStatusEnum.REFUNDED);
            updateById(order);
            // 入账 + 回库存放在钱包服务，与支付对称
            walletRecordService.refundCredit(order, items);
            return;
        }

        // 驳回：恢复申请前状态，清空 status_before_refund
        OrderStatusEnum restore = order.getStatusBeforeRefund();
        if (restore == null) {
            restore = order.getFulfillmentType() == FulfillmentTypeEnum.VIRTUAL
                    ? OrderStatusEnum.TO_USE
                    : OrderStatusEnum.TO_SHIP;
        }
        int rows = tradeOrderMapper.casUpdateStatus(id, OrderStatusEnum.REFUNDING, restore);
        if (rows != 1) {
            throw new BusinessException("驳回退款失败，请刷新后重试");
        }
        order.setStatus(restore);
        order.setStatusBeforeRefund(null);
        if (StringUtils.hasText(dto.getRejectReason())) {
            order.setRefundReason((order.getRefundReason() == null ? "" : order.getRefundReason() + " | ")
                    + "驳回：" + dto.getRejectReason());
        }
        updateById(order);
    }

    /**
     * 管理员模拟物流「已送达」：待收货 → 待签收
     * MVP 无真实物流回调，用此接口推进实物状态机
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeliver(Long id) {
        SecurityUtil.requireAdmin();
        TradeOrder order = getById(id);
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new BusinessException("订单不存在");
        }
        int rows = tradeOrderMapper.casUpdateStatus(id, OrderStatusEnum.TO_RECEIVE, OrderStatusEnum.TO_SIGN);
        if (rows != 1) {
            throw new BusinessException("仅待收货订单可标记送达");
        }
        order.setStatus(OrderStatusEnum.TO_SIGN);
        order.setDeliverTime(LocalDateTime.now());
        updateById(order);
    }

    // ======================== 内部：解析下单行 / 校验 ========================

    /**
     * 把下单入参统一成「商品行」列表：
     * - fromCart=true：取当前用户购物车 selected=1 的项
     * - 否则：立即购买 productId + quantity
     * 此处只做可读库存校验；真正防超卖在支付 CAS
     */
    private List<Line> resolveLines(OrderSubmitDTO dto, Long userId) {
        if (dto == null) {
            throw new BusinessException("下单参数不能为空");
        }
        List<Line> lines = new ArrayList<>();
        if (Boolean.TRUE.equals(dto.getFromCart())) {
            List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getSelected, 1));
            if (cartItems.isEmpty()) {
                throw new BusinessException("购物车没有勾选商品");
            }
            for (CartItem cartItem : cartItems) {
                ShopProduct product = requireOnSale(cartItem.getProductId());
                int qty = cartItem.getQuantity() == null ? 1 : cartItem.getQuantity();
                if (product.getStock() == null || product.getStock() < qty) {
                    throw new BusinessException("库存不足：" + product.getName());
                }
                FulfillmentTypeEnum ft = FulfillmentTypeEnum.fromProductType(product.getType());
                if (ft == FulfillmentTypeEnum.VIRTUAL) {
                    throw new BusinessException("门票/核销票请直接下单，不可从购物车结算");
                }
                lines.add(new Line(product, qty, ft));
            }
            requireSameSeller(lines);
        } else {
            if (dto.getProductId() == null) {
                throw new BusinessException("请指定商品或从购物车结算");
            }
            int qty = dto.getQuantity() == null ? 1 : dto.getQuantity();
            if (qty < 1) {
                throw new BusinessException("购买数量至少为 1");
            }
            ShopProduct product = requireOnSale(dto.getProductId());
            if (product.getStock() == null || product.getStock() < qty) {
                throw new BusinessException("库存不足：" + product.getName());
            }
            lines.add(new Line(product, qty, FulfillmentTypeEnum.fromProductType(product.getType())));
        }
        return lines;
    }

    /** 单笔订单履约类型必须一致（虚拟与实物拆单） */
    private FulfillmentTypeEnum requireSameFulfillment(List<Line> lines) {
        FulfillmentTypeEnum first = lines.get(0).fulfillment;
        for (Line line : lines) {
            if (line.fulfillment == null) {
                throw new BusinessException("商品履约类型异常");
            }
            if (line.fulfillment != first) {
                throw new BusinessException("虚拟与实物商品不可同一单结算，请分开下单");
            }
        }
        return first;
    }

    /** 购物车结算：同一单只能同一个卖家，便于农户/村长一对一处理退款等 */
    private void requireSameSeller(List<Line> lines) {
        Long firstSeller = lines.get(0).product.getUserId();
        if (firstSeller == null) {
            throw new BusinessException("商品卖家信息异常");
        }
        for (Line line : lines) {
            if (!firstSeller.equals(line.product.getUserId())) {
                throw new BusinessException("不同商家的商品请分开结算");
            }
        }
    }

    /** 商品须存在、上架、价格与类型齐全 */
    private ShopProduct requireOnSale(Long productId) {
        ShopProduct product = shopProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException("商品已下架：" + product.getName());
        }
        if (product.getPrice() == null || product.getType() == null) {
            throw new BusinessException("商品数据异常");
        }
        return product;
    }

    private UserAddress requireOwnAddress(Long addressId, Long userId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new BusinessException("收货地址不存在");
        }
        return address;
    }

    /** 买家视角：本人 + 未软删 */
    private TradeOrder requireOwnOrder(Long id) {
        Long userId = UserHolder.getUser().getId();
        TradeOrder order = getById(id);
        if (order == null || !userId.equals(order.getUserId()) || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    /** 卖家视角：当前用户须出现在本单任一明细的 seller_user_id */
    private TradeOrder requireSellerOrder(Long id) {
        Long sellerId = UserHolder.getUser().getId();
        TradeOrder order = getById(id);
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new BusinessException("订单不存在");
        }
        Long count = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id)
                .eq(OrderItem::getSellerUserId, sellerId));
        if (count == null || count == 0) {
            throw new BusinessException("无权操作该订单");
        }
        return order;
    }

    private List<OrderItem> loadItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    /** 分页结果批量挂明细，避免 N+1 */
    private PageResultVO<TradeOrderVO> toPageVo(Page<TradeOrder> page) {
        List<TradeOrder> records = page.getRecords();
        if (records.isEmpty()) {
            return new PageResultVO<>(page.getTotal(), Collections.emptyList());
        }
        List<Long> orderIds = records.stream().map(TradeOrder::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
                ).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        List<TradeOrderVO> vos = records.stream()
                .map(o -> toVo(o, itemsByOrderId.getOrDefault(o.getId(), Collections.emptyList())))
                .toList();
        return new PageResultVO<>(page.getTotal(), vos);
    }

    private TradeOrderVO toVo(TradeOrder order, List<OrderItem> items) {
        TradeOrderVO vo = BeanUtil.copyProperties(order, TradeOrderVO.class);
        vo.setItems(items.stream()
                .map(item -> BeanUtil.copyProperties(item, TradeOrderVO.OrderItemVO.class))
                .toList());
        return vo;
    }

    /** 对外单号：SY + yyyyMMddHHmmss + 4 位随机数 */
    private String genOrderNo() {
        return "SY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + RandomUtil.randomNumbers(4);
    }

    private int pageNo(PageResultDTO dto) {
        return dto != null && dto.getPageNo() != null && dto.getPageNo() > 0 ? dto.getPageNo() : 1;
    }

    private int pageSize(PageResultDTO dto) {
        return dto != null && dto.getPageSize() != null && dto.getPageSize() > 0 ? dto.getPageSize() : 10;
    }

    /** 内部下单行：已校验的商品 + 数量 + 履约类型 */
    private record Line(ShopProduct product, int quantity, FulfillmentTypeEnum fulfillment) {
        BigDecimal subtotal() {
            return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }
}

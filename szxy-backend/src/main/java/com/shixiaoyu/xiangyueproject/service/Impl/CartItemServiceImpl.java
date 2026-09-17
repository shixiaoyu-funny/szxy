package com.shixiaoyu.xiangyueproject.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.dto.CartAddDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.CartAlterDTO;
import com.shixiaoyu.xiangyueproject.entity.po.CartItem;
import com.shixiaoyu.xiangyueproject.entity.po.ShopProduct;
import com.shixiaoyu.xiangyueproject.entity.vo.CartItemVO;
import com.shixiaoyu.xiangyueproject.enums.FulfillmentTypeEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.CartItemMapper;
import com.shixiaoyu.xiangyueproject.mapper.ShopProductMapper;
import com.shixiaoyu.xiangyueproject.service.CartItemService;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 * <p>
 * 只挂 shop_product；履约类型由商品 type 推导后冗余到 cart_item，便于结算时校验「不可虚拟+实物混单」。
 */
@Service
@RequiredArgsConstructor
public class CartItemServiceImpl extends ServiceImpl<CartItemMapper, CartItem>
        implements CartItemService {

    private final ShopProductMapper shopProductMapper;

    /**
     * 当前用户购物车列表，并批量带上商品展示字段（名称/图/价/库存等）
     */
    @Override
    public List<CartItemVO> ls() {
        Long userId = UserHolder.getUser().getId();
        List<CartItem> items = lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getUpdateTime)
                .list();
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        // 一次查出相关商品，避免 N+1
        List<Long> productIds = items.stream().map(CartItem::getProductId).distinct().toList();
        Map<Long, ShopProduct> productMap = shopProductMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(ShopProduct::getId, Function.identity(), (a, b) -> a));

        return items.stream().map(item -> toVo(item, productMap.get(item.getProductId()))).toList();
    }

    /**
     * 加入购物车：同用户同商品已存在则累加数量，否则新建一行
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CartAddDTO dto) {
        if (dto == null || dto.getProductId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        if (quantity < 1) {
            throw new BusinessException("购买数量至少为 1");
        }

        ShopProduct product = requireOnSaleProduct(dto.getProductId());
        FulfillmentTypeEnum fulfillmentType = FulfillmentTypeEnum.fromProductType(product.getType());
        if (fulfillmentType == FulfillmentTypeEnum.VIRTUAL) {
            throw new BusinessException("门票/核销票请直接下单，不可加入购物车");
        }
        if (product.getStock() != null && product.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }

        Long userId = UserHolder.getUser().getId();
        // uk_user_product：同一用户同一商品最多一行
        CartItem existing = lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, product.getId())
                .one();

        if (existing == null) {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(product.getId());
            item.setFulfillmentType(fulfillmentType);
            item.setQuantity(quantity);
            item.setSelected(1); // 新加入默认勾选，方便直接去结算
            save(item);
            return;
        }

        // 已存在：累加数量后再校验库存
        int newQty = existing.getQuantity() + quantity;
        if (product.getStock() != null && product.getStock() < newQty) {
            throw new BusinessException("库存不足");
        }
        existing.setQuantity(newQty);
        existing.setFulfillmentType(fulfillmentType);
        existing.setSelected(1);
        updateById(existing);
    }

    /**
     * 改数量和/或勾选状态；至少传其中一个字段
     */
    @Override
    public void alter(Long id, CartAlterDTO dto) {
        if (dto == null) {
            throw new BusinessException("修改内容不能为空");
        }
        if (dto.getQuantity() == null && dto.getSelected() == null) {
            throw new BusinessException("请至少修改数量或勾选状态");
        }
        CartItem item = requireOwnItem(id);
        if (dto.getQuantity() != null) {
            if (dto.getQuantity() < 1) {
                throw new BusinessException("购买数量至少为 1");
            }
            ShopProduct product = shopProductMapper.selectById(item.getProductId());
            if (product != null && product.getStock() != null && product.getStock() < dto.getQuantity()) {
                throw new BusinessException("库存不足");
            }
            item.setQuantity(dto.getQuantity());
        }
        if (dto.getSelected() != null) {
            if (!Objects.equals(dto.getSelected(), 0) && !Objects.equals(dto.getSelected(), 1)) {
                throw new BusinessException("勾选状态只能为 0 或 1");
            }
            item.setSelected(dto.getSelected());
        }
        updateById(item);
    }

    /** 删除当前用户的某一购物车项 */
    @Override
    public void del(Long id) {
        requireOwnItem(id);
        removeById(id);
    }

    /** 清空当前用户购物车 */
    @Override
    public void clear() {
        Long userId = UserHolder.getUser().getId();
        lambdaUpdate().eq(CartItem::getUserId, userId).remove();
    }

    /** 校验购物车项存在且属于当前用户 */
    private CartItem requireOwnItem(Long id) {
        Long userId = UserHolder.getUser().getId();
        CartItem item = getById(id);
        if (item == null || !userId.equals(item.getUserId())) {
            throw new BusinessException("购物车项不存在");
        }
        return item;
    }

    /** 校验商品存在、上架、类型合法（下架不可加购） */
    private ShopProduct requireOnSaleProduct(Long productId) {
        ShopProduct product = shopProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException("商品已下架");
        }
        if (product.getType() == null) {
            throw new BusinessException("商品类型异常");
        }
        return product;
    }

    /** cart_item + shop_product 拼装列表 VO */
    private CartItemVO toVo(CartItem item, ShopProduct product) {
        CartItemVO vo = new CartItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setFulfillmentType(item.getFulfillmentType());
        vo.setQuantity(item.getQuantity());
        vo.setSelected(item.getSelected());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getImage());
            vo.setPrice(product.getPrice());
            vo.setProductType(product.getType());
            vo.setStock(product.getStock());
            vo.setProductStatus(product.getStatus());
            vo.setSellerUserId(product.getUserId());
        }
        return vo;
    }
}

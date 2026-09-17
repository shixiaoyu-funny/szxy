package com.shixiaoyu.xiangyueproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shixiaoyu.xiangyueproject.entity.po.ShopProduct;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 可售商品 mapper（含库存乐观扣减）
 */
public interface ShopProductMapper extends BaseMapper<ShopProduct> {

    /**
     * 乐观扣库存：仅当库存充足且上架时成功（影响行数=1）
     */
    @Update("UPDATE shop_product SET stock = stock - #{qty} " +
            "WHERE id = #{id} AND status = 1 AND stock >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

    /**
     * 退款/取消后回补库存
     */
    @Update("UPDATE shop_product SET stock = stock + #{qty} WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);
}

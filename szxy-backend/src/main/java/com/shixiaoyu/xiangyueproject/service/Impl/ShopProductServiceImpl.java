package com.shixiaoyu.xiangyueproject.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.po.ShopProduct;
import com.shixiaoyu.xiangyueproject.mapper.ShopProductMapper;
import com.shixiaoyu.xiangyueproject.service.ShopProductService;
import org.springframework.stereotype.Service;

/**
 * 可售商品服务实现
 */
@Service
public class ShopProductServiceImpl extends ServiceImpl<ShopProductMapper, ShopProduct>
        implements ShopProductService {
}

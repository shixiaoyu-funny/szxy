package com.shixiaoyu.xiangyueproject.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.entity.po.ShopProduct;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.ShopProductVO;
import com.shixiaoyu.xiangyueproject.mapper.ShopProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 可售商品查询（景点详情挂购）
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Tag(name = "商品")
public class ProductController {

    private final ShopProductMapper shopProductMapper;

    /** 某景点下全部上架商品（门票/住宿核销/实体） */
    @GetMapping("/scenic/{scenicId}")
    @Operation(summary = "按景点查询上架商品")
    public Result<List<ShopProductVO>> byScenic(
            @Parameter(description = "景点ID", required = true) @PathVariable Long scenicId) {
        List<ShopProduct> list = shopProductMapper.selectList(new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getScenicId, scenicId)
                .eq(ShopProduct::getStatus, 1)
                .orderByAsc(ShopProduct::getType)
                .orderByAsc(ShopProduct::getPrice));
        return Result.ok(list.stream().map(p -> BeanUtil.copyProperties(p, ShopProductVO.class)).toList());
    }
}

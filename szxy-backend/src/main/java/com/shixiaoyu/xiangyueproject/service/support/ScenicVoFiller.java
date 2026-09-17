package com.shixiaoyu.xiangyueproject.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.entity.po.ShopProduct;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.enums.ProductTypeEnum;
import com.shixiaoyu.xiangyueproject.mapper.ShopProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 景点 VO：按 scenic_id + 门票核销类型回填展示价与商品ID
 */
@Component
@RequiredArgsConstructor
public class ScenicVoFiller {

    private final ShopProductMapper shopProductMapper;

    /**
     * 批量填充门票价 + ticketProductId（上架门票核销；同景点多条时取最低价那条）
     */
    public void fillTicketPrice(List<ScenicVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        Set<Long> scenicIds = voList.stream().map(ScenicVO::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (scenicIds.isEmpty()) {
            return;
        }
        // 同景点可能多条门票：按价格升序，保留最便宜的一条供展示/购买
        Map<Long, ShopProduct> ticketByScenic = shopProductMapper.selectList(new LambdaQueryWrapper<ShopProduct>()
                        .in(ShopProduct::getScenicId, scenicIds)
                        .eq(ShopProduct::getType, ProductTypeEnum.TICKET)
                        .eq(ShopProduct::getStatus, 1))
                .stream()
                .filter(p -> p.getScenicId() != null && p.getPrice() != null)
                .sorted(Comparator.comparing(ShopProduct::getPrice))
                .collect(Collectors.toMap(ShopProduct::getScenicId, p -> p, (a, b) -> a));

        voList.forEach(v -> {
            ShopProduct p = ticketByScenic.get(v.getId());
            if (p == null) {
                v.setPrice(null);
                v.setTicketProductId(null);
                return;
            }
            v.setPrice(p.getPrice().setScale(0, RoundingMode.HALF_UP).intValue());
            v.setTicketProductId(p.getId());
        });
    }
}

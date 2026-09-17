package com.shixiaoyu.xiangyueproject.common.tools;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.po.ShopProduct;
import com.shixiaoyu.xiangyueproject.enums.OrderStatusEnum;
import com.shixiaoyu.xiangyueproject.service.CartItemService;
import com.shixiaoyu.xiangyueproject.service.ShopProductService;
import com.shixiaoyu.xiangyueproject.service.TradeOrderService;
import com.shixiaoyu.xiangyueproject.service.WalletRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.shixiaoyu.xiangyueproject.common.config.AIConfiguration.logToolInvoked;

/**
 * 订单 / 购物车 / 近期消费 AI 工具：只调现有 Service，结果转 JSON 交给模型归纳。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAndCartTool {

    private final TradeOrderService tradeOrderService;
    private final CartItemService cartItemService;
    private final WalletRecordService walletRecordService;
    private final ShopProductService shopProductService;

    @Tool(name = "get_user_orders", description = """
            调用时机：用户询问自己的订单、待支付/待发货/待收货/待使用、退款、订单金额、物流履约等情况，
            或回答需要结合用户真实订单数据时。
            功能：查询当前登录用户的订单列表（含明细）。可按状态筛选。
            订单状态码：1待支付 2已取消 3待使用 4待发货 5待收货 6待签收 7已签收 8已使用 9退款中 10已退款；不传则查全部。
            """)
    public String getUserOrders(
            @ToolParam(description = "页码，默认1", required = false) Integer pageNo,
            @ToolParam(description = "每页条数，默认10，最大20", required = false) Integer pageSize,
            @ToolParam(description = "订单状态码（可选）", required = false) Integer status
    ) {
        logToolInvoked("get_user_orders");
        PageResultDTO page = pageOf(pageNo, pageSize, 10, 20);
        OrderStatusEnum statusEnum = null;
        if (status != null) {
            try {
                statusEnum = OrderStatusEnum.fromCode(status);
            } catch (IllegalArgumentException e) {
                return JSONUtil.toJsonStr(Map.of(
                        "ok", false,
                        "message", "非法订单状态码: " + status
                ));
            }
        }
        return JSONUtil.toJsonStr(tradeOrderService.ls(page, statusEnum));
    }

    @Tool(name = "get_user_cart", description = """
            调用时机：用户询问购物车里有什么、勾选了哪些、数量/金额、能否结算，
            或推荐/提醒时需要结合购物车现有商品。
            功能：查询当前登录用户购物车全部商品。
            """)
    public String getUserCart() {
        logToolInvoked("get_user_cart");
        return JSONUtil.toJsonStr(cartItemService.ls());
    }

    @Tool(name = "get_recent_shopping_and_recommend", description = """
            调用时机：用户问起最近买了什么、支付/消费记录、想再买类似商品；
            或需要按用户近期购物偏好推荐类型相近、价位相仿、可能喜欢的商品时。
            功能：返回近期钱包消费流水、近期订单，以及一批在售商品供你挑选推荐；请根据消费偏好自行筛选并说明理由。
            """)
    public String getRecentShoppingAndRecommend(
            @ToolParam(description = "分页条数（流水与订单），默认8，最大15", required = false) Integer limit,
            @ToolParam(description = "候选在售商品条数，默认20，最大40", required = false) Integer productLimit
    ) {
        logToolInvoked("get_recent_shopping_and_recommend");
        int n = limit == null || limit < 1 ? 8 : Math.min(limit, 15);
        int pN = productLimit == null || productLimit < 1 ? 20 : Math.min(productLimit, 40);
        PageResultDTO page = pageOf(1, n, n, n);

        // 在售商品候选：推荐算法交给模型，工具只提供候选池
        var onSale = shopProductService.list(new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getStatus, 1)
                .gt(ShopProduct::getStock, 0)
                .orderByDesc(ShopProduct::getUpdateTime)
                .last("limit " + pN));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("wallet", walletRecordService.recordsSplit(page));
        resp.put("recentOrders", tradeOrderService.ls(page, null));
        resp.put("onSaleProducts", onSale);
        resp.put("tip", "请结合 wallet.consume 与 recentOrders 判断偏好，从 onSaleProducts 中挑选类型/价位相仿的商品推荐，并说明理由。");
        return JSONUtil.toJsonStr(resp);
    }

    private static PageResultDTO pageOf(Integer pageNo, Integer pageSize, int defaultSize, int maxSize) {
        PageResultDTO page = new PageResultDTO();
        page.setPageNo(pageNo == null || pageNo < 1 ? 1 : pageNo);
        int size = pageSize == null || pageSize < 1 ? defaultSize : pageSize;
        page.setPageSize(Math.min(size, maxSize));
        return page;
    }
}

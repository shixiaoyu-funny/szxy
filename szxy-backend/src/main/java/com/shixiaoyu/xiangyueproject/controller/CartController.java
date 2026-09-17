package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.CartAddDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.CartAlterDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.CartItemVO;
import com.shixiaoyu.xiangyueproject.service.CartItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车简单 CRUD（登录态；仅挂 shop_product）
 */
@RestController
@RequestMapping("/cart")
@Slf4j
@Tag(name = "购物车")
@RequiredArgsConstructor
public class CartController {

    private final CartItemService cartItemService;

    /** 列表（含商品名称/单价/库存等展示字段） */
    @GetMapping("/ls")
    @Operation(summary = "购物车列表")
    public Result<List<CartItemVO>> ls() {
        return Result.ok(cartItemService.ls());
    }

    /** 加购；同商品已存在则累加数量 */
    @PostMapping("/add")
    @Operation(summary = "加入购物车")
    public Result<Void> add(@RequestBody CartAddDTO dto) {
        cartItemService.add(dto);
        return Result.ok();
    }

    /** 改数量和/或勾选；body 至少带一个字段 */
    @PostMapping("/alter/{id}")
    @Operation(summary = "修改数量或勾选")
    public Result<Void> alter(
            @Parameter(description = "购物车项ID", required = true) @PathVariable Long id,
            @RequestBody CartAlterDTO dto) {
        cartItemService.alter(id, dto);
        return Result.ok();
    }

    /** 删除单项 */
    @PostMapping("/del/{id}")
    @Operation(summary = "删除单项")
    public Result<Void> del(@Parameter(description = "购物车项ID", required = true) @PathVariable Long id) {
        cartItemService.del(id);
        return Result.ok();
    }

    /** 清空当前用户全部购物车项 */
    @PostMapping("/clear")
    @Operation(summary = "清空购物车")
    public Result<Void> clear() {
        cartItemService.clear();
        return Result.ok();
    }
}

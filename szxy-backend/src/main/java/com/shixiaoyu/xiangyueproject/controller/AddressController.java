package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.UserAddressDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.UserAddressVO;
import com.shixiaoyu.xiangyueproject.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址 CRUD（登录态；Service 内按当前用户隔离）
 */
@RestController
@RequestMapping("/address")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "地址")
public class AddressController {

    private final UserAddressService addressService;

    /** 当前用户全部地址（默认在前） */
    @GetMapping("/ls")
    @Operation(summary = "当前用户地址列表")
    public Result<List<UserAddressVO>> ls() {
        return Result.ok(addressService.ls());
    }

    /** 修改地址；dto 需带 id，可顺带改 isDefault */
    @PostMapping("/alter")
    @Operation(summary = "修改地址")
    public Result<Void> alterAd(
            @Parameter(description = "修改后的地址表单信息", required = true) @RequestBody UserAddressDTO dto) {
        addressService.alterAd(dto);
        return Result.ok();
    }

    /** 将指定地址设为默认（同用户其余默认会被清掉） */
    @PostMapping("/default/{id}")
    @Operation(summary = "设为默认地址")
    public Result<Void> setDefault(
            @Parameter(description = "地址id", required = true) @PathVariable Long id) {
        addressService.setDefault(id);
        return Result.ok();
    }

    /** 删除非默认地址；默认地址需先换默认再删 */
    @PostMapping("/del/{id}")
    @Operation(summary = "删除非默认地址")
    public Result<Void> del(
            @Parameter(description = "地址id", required = true) @PathVariable Long id) {
        addressService.del(id);
        return Result.ok();
    }

    /** 新增；用户首条地址会强制设为默认 */
    @PostMapping("/add")
    @Operation(summary = "新增地址")
    public Result<Void> add(
            @Parameter(description = "新增地址表单信息", required = true) @RequestBody UserAddressDTO dto) {
        addressService.add(dto);
        return Result.ok();
    }
}

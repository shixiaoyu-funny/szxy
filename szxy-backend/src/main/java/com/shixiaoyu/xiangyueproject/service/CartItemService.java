package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.CartAddDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.CartAlterDTO;
import com.shixiaoyu.xiangyueproject.entity.po.CartItem;
import com.shixiaoyu.xiangyueproject.entity.vo.CartItemVO;

import java.util.List;

/**
 * 购物车服务
 */
public interface CartItemService extends IService<CartItem> {
    List<CartItemVO> ls();

    void add(CartAddDTO dto);

    void alter(Long id, CartAlterDTO dto);

    void del(Long id);

    void clear();
}

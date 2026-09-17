package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.UserAddressDTO;
import com.shixiaoyu.xiangyueproject.entity.po.UserAddress;
import com.shixiaoyu.xiangyueproject.entity.vo.UserAddressVO;

import java.util.List;

/**
 * 用户收货地址服务
 */
public interface UserAddressService extends IService<UserAddress> {
    List<UserAddressVO> ls();

    void alterAd(UserAddressDTO dto);

    void setDefault(Long id);

    void del(Long id);

    void add(UserAddressDTO dto);
}

package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.dto.UserAddressDTO;
import com.shixiaoyu.xiangyueproject.entity.po.UserAddress;
import com.shixiaoyu.xiangyueproject.entity.vo.UserAddressVO;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.UserAddressMapper;
import com.shixiaoyu.xiangyueproject.service.UserAddressService;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户收货地址服务实现
 * <p>
 * 约定：
 * 1. 所有操作只作用于当前登录用户（UserHolder）
 * 2. 同一用户同一时刻最多一条默认地址（is_default=1）
 * 3. 默认地址不允许直接删除，需先把其他地址设为默认
 */
@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress>
        implements UserAddressService {

    /**
     * 查询当前用户全部地址
     * 排序：默认地址优先，再按最近更新时间倒序（方便确认页直接取第一条当默认）
     */
    @Override
    public List<UserAddressVO> ls() {
        Long userId = UserHolder.getUser().getId();
        List<UserAddress> list = lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime)
                .list();
        return list.stream()
                .map(ad -> BeanUtil.copyProperties(ad, UserAddressVO.class))
                .toList();
    }

    /**
     * 修改地址（需传 dto.id）
     * 前端约定：表单回填后勾选默认 → isDefault=1，不勾选 → 明确传 0（不做“未传保留原值”）
     * 勾选为默认时：先清同用户其他默认，再更新本条
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void alterAd(UserAddressDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("地址ID不能为空");
        }
        validateForm(dto);
        if (dto.getIsDefault() == null
                || (!Integer.valueOf(0).equals(dto.getIsDefault()) && !Integer.valueOf(1).equals(dto.getIsDefault()))) {
            throw new BusinessException("是否默认地址须传 0 或 1");
        }
        // 归属校验：只能改自己的地址
        UserAddress existing = requireOwnAddress(dto.getId());

        // 勾选默认：先清旧默认，再写本条为 1
        if (Integer.valueOf(1).equals(dto.getIsDefault())) {
            clearDefault(existing.getUserId());
        }

        UserAddress toUpdate = BeanUtil.copyProperties(dto, UserAddress.class);
        toUpdate.setUserId(existing.getUserId()); // 防止前端误改归属
        toUpdate.setIsDefault(dto.getIsDefault());
        updateById(toUpdate);
    }

    /**
     * 单独设默认：目标地址置 1，同用户其余默认清 0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        UserAddress address = requireOwnAddress(id);
        clearDefault(address.getUserId());
        address.setIsDefault(1);
        updateById(address);
    }

    /**
     * 删除地址；默认地址禁止删，避免用户无默认地址
     */
    @Override
    public void del(Long id) {
        UserAddress address = requireOwnAddress(id);
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            throw new BusinessException("当前地址为默认地址，不允许删除，请先设置其他地址为默认");
        }
        removeById(id);
    }

    /**
     * 新增地址
     * - 用户第一条地址：无论表单是否勾选，都强制为默认
     * - 勾选默认时：先 clearDefault 再插入
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(UserAddressDTO dto) {
        if (dto == null) {
            throw new BusinessException("地址信息不能为空");
        }
        validateForm(dto);
        Long userId = UserHolder.getUser().getId();
        long count = lambdaQuery().eq(UserAddress::getUserId, userId).count();

        int isDefault = dto.getIsDefault() != null && dto.getIsDefault() == 1 ? 1 : 0;
        // 首条地址强制默认，保证下单确认页总能取到默认地址
        if (count == 0) {
            isDefault = 1;
        }
        if (isDefault == 1) {
            clearDefault(userId);
        }

        UserAddress address = BeanUtil.copyProperties(dto, UserAddress.class);
        address.setId(null); // 避免误带主键走更新
        address.setUserId(userId);
        address.setIsDefault(isDefault);
        save(address);
    }

    /**
     * 按 id 取地址，并校验属于当前登录用户；不存在或不属于自己一律按「不存在」处理
     */
    private UserAddress requireOwnAddress(Long id) {
        Long userId = UserHolder.getUser().getId();
        UserAddress address = getById(id);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new BusinessException("地址不存在");
        }
        return address;
    }

    /**
     * 将该用户下所有 is_default=1 清成 0（设新默认前调用）
     */
    private void clearDefault(Long userId) {
        lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0)
                .update();
    }

    /** 新增/修改共用的必填校验 */
    private void validateForm(UserAddressDTO dto) {
        if (!StringUtils.hasText(dto.getReceiverName())) {
            throw new BusinessException("收件人不能为空");
        }
        if (!StringUtils.hasText(dto.getPhone())) {
            throw new BusinessException("手机号不能为空");
        }
        if (!StringUtils.hasText(dto.getProvince())
                || !StringUtils.hasText(dto.getCity())
                || !StringUtils.hasText(dto.getCounty())
                || !StringUtils.hasText(dto.getDetail())) {
            throw new BusinessException("省市区与详细地址不能为空");
        }
    }
}

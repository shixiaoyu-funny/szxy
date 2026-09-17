package com.shixiaoyu.xiangyueproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户表 mapper（含余额乐观加减）
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 乐观扣余额：余额不足时影响行数=0
     */
    @Update("UPDATE `user` SET balance = balance - #{amount} " +
            "WHERE id = #{id} AND balance >= #{amount}")
    int deductBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 充值 / 退款入账
     */
    @Update("UPDATE `user` SET balance = IFNULL(balance, 0) + #{amount} WHERE id = #{id}")
    int addBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);
}

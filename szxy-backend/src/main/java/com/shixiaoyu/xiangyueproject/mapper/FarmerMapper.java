package com.shixiaoyu.xiangyueproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerUserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 农户档案 mapper
 */
public interface FarmerMapper extends BaseMapper<FarmerUser> {

    /** 查询某村落下的全部农户（联查 user 取用户名/手机号） */
    List<FarmerUserVO> selectFarmersByVillage(@Param("villageId") Long villageId);

    /** 管理端查询全部农户（联查 user） */
    List<FarmerUserVO> selectAllFarmers();
}

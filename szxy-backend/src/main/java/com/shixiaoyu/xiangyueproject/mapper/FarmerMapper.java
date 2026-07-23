package com.shixiaoyu.xiangyueproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerAccessVO;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerUserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface FarmerMapper extends BaseMapper<FarmerUser> {
    void applyManager(FarmerAccessDTO farmerAccessDTO);

    Long getVillageIdByManageId(@Param("manageId") Long manageId);

    List<FarmerUserVO> selectFarmersByVillage(@Param("villageId") Long villageId);

    // 1. 查找当前用户作为“村长”(type=2) 关联的村落ID
    Long getVillageIdByChiefId(@Param("userId") Long userId);

    // 2. 查找该村落下所有“农户”(type=1) 的详细信息 (联查 user 表取姓名电话)
    List<FarmerUserVO> selectFarmersByVillageAndType(@Param("villageId") Long villageId);

    // 1. 获取特定村落的农户资质申请列表（联查 user 表）
    List<FarmerAccessVO> selectFarmerAccessByVillage(@Param("villageId") Long villageId);

    // 获取所有农户列表（管理端）
    List<FarmerUserVO> selectAllFarmers();

    // 获取所有农户资质申请列表（管理端）
    List<FarmerAccessVO> selectAllFarmerAccess();
}

package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerUserDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerUserVO;

import java.util.List;

public interface FarmerService extends IService<FarmerUser> {
    Result applyManager(FarmerAccessDTO farmerAccessDTO);

    Result<List<FarmerUserVO>> getFarmersByVillage();

    Result addFarmer(Long villageId, FarmerUserDTO farmerUserDTO);

    Result updateFarmer(Long id, FarmerUserDTO farmerUserDTO);

    Result deleteFarmer(Long id);

    Result setVillageManager(Long villageId, Long farmerId);

    Result getFarmerAccessList();

    Result getManagerAccessList();

    // 管理端专用方法
    Result<List<FarmerUserVO>> getAllFarmers();

    Result getAllFarmerAccessList();

    Result solveFarmer(Long id, Integer status,Long villageId);

    Result solveManager(Long id, Integer status);

    Result solveScenic(Long id, Integer status);
}

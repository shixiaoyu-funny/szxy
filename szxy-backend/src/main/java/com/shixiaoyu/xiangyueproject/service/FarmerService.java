package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerUserDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerUserVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;

import java.util.List;

/**
 * 农户服务：建档/本村管理/村长任命（错误时抛 BusinessException）
 */
public interface FarmerService extends IService<FarmerUser> {

    /** 村长：本村农户列表 */
    List<FarmerUserVO> getFarmersByVillage();

    /** 村长：本村新增农户（建 user 账号 + farm_user 档案） */
    void addFarmer(Long villageId, FarmerUserDTO farmerUserDTO);

    /** 村长：修改本村农户 */
    void updateFarmer(Long farmerUserId, FarmerUserDTO farmerUserDTO);

    /** 村长：删除本村农户（删档案，账号降为游客） */
    void deleteFarmer(Long farmerUserId);

    /** 管理端：全量农户列表 */
    List<FarmerUserVO> getAllFarmers();

    /** 管理端：建档农户（可指定任意村落） */
    void createFarmer(Long villageId, FarmerUserDTO farmerUserDTO);

    /** 管理端：任命/更换村长（事务内同步 village_base.manage_id 与新旧 role） */
    void setVillageManager(Long villageId, Long farmerUserId);

    /** 农户/村长：我的村信息 */
    VillageBaseVO getMyVillage();

    /** 农户/村长：我的景点列表 */
    List<ScenicVO> getMyScenics();
}

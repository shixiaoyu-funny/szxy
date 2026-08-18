package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.VillageBaseDTO;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;

import java.util.List;

/**
 * 农村信息服务（管理端 CRUD + top10 排行，错误时抛 BusinessException）
 */
public interface VillageService extends IService<VillageBase> {

    PageResultVO<VillageBaseVO> villageList(PageResultDTO pageResultDTO);

    void addVillage(VillageBaseDTO villageBaseDTO);

    void updateVillage(Long id, VillageBaseDTO villageBaseDTO);

    void deleteVillage(Long id);

    /** 优质农村 top10（按下属景点总点赞量） */
    List<VillageBaseVO> villageLikes();

    /** 优质农村 top10（按下属景点总收藏量） */
    List<VillageBaseVO> villageCollections();
}

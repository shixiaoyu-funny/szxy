package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.VillageBaseDTO;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;

import java.util.List;

public interface VillageService extends IService<VillageBase> {
    Result<PageResultVO<VillageBaseVO>> villageList(PageResultDTO pageResultDTO);

    Result addVillage(VillageBaseDTO villageBaseDTO);

    Result updateVillage(Long id, VillageBaseDTO villageBaseDTO);

    Result<List<VillageBaseVO>> villageLikes();

    Result<List<VillageBaseVO>> villageCollections();
}

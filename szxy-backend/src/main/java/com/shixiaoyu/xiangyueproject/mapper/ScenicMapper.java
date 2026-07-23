package com.shixiaoyu.xiangyueproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

public interface ScenicMapper extends BaseMapper<VillageScenic> {
    List<Map<String,Object>> selectLikes(List<Long> idList);

    List<Map<String,Object>> selectCollections(List<Long> idList);
}

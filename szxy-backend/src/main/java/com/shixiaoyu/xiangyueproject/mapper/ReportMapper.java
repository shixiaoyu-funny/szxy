package com.shixiaoyu.xiangyueproject.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 报表统计 mapper
 */
public interface ReportMapper {

    /** 农户总数 */
    Integer farmCnt();

    /** 某日期（含）之后新增的农村数量 */
    Integer countNewVillage(@Param("date") String date);

    /** 某日期（含）之后新增的景点数量 */
    Integer countNewScenic(@Param("date") String date);
}

package com.shixiaoyu.xiangyueproject.service;

/**
 * 报表服务（错误时抛 BusinessException）
 */
public interface ReportService {

    /** 农户总数 */
    Integer farmCnt();

    /** 当日新增农村数 */
    Integer newVillageCount();

    /** 当日新增景点数 */
    Integer newScenicCount();
}

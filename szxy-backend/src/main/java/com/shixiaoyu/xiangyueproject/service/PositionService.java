package com.shixiaoyu.xiangyueproject.service;

import com.shixiaoyu.xiangyueproject.entity.dto.PosReportDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.PositionDTO;
import com.shixiaoyu.xiangyueproject.entity.vo.PositionVO;

/**
 * 省市区经纬度 Redis 缓存服务（不调用高德，仅读写缓存）
 */
public interface PositionService {

    /**
     * 按省市区从 Redis 读取经纬度。
     * key = pos:{province}:{city}:{county}，value = "经度,纬度"。
     * 未命中时返回的 VO 中 longitude/latitude 为 null。
     */
    PositionVO getPos(String province, String city, String county);

    /**
     * 将经纬度写入 Redis（String，无 TTL）。
     * value 格式与高德 location 一致：经度,纬度
     */
    void savePos(PositionDTO dto);

    /**
     * 登录用户上报当前定位：与 Redis 上次位置比对，变更时按开关投递推荐 MQ。
     */
    void reportPos(PosReportDTO dto);
}

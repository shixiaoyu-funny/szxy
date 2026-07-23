package com.shixiaoyu.xiangyueproject.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class PVUVUtils {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 日期格式化（固定按天统计，不用改）
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ===================== 1. PV统计（访问一次+1）=====================
    /**
     * 记录PV（每次页面访问调用1次）
     * @param date 统计日期（传当天日期即可）
     */
    public void incrPV(LocalDate date) {
        String pvKey = "pv:" + date.format(DATE_FORMAT);
        stringRedisTemplate.opsForValue().increment(pvKey, 1); // 原子自增
        stringRedisTemplate.expire(pvKey, 7, java.util.concurrent.TimeUnit.DAYS);
    }

    /**
     * 查询PV（直接传日期就能查）
     */
    public Long getPV(LocalDate date) {
        String pvKey = "pv:" + date.format(DATE_FORMAT);
        String value = stringRedisTemplate.opsForValue().get(pvKey);
        return value == null ? 0 : Long.parseLong(value);
    }

    // ===================== 2. UV统计（同一用户只算1次）=====================
    /**
     * 记录UV（用户访问时调用，传用户唯一标识）
     * @param date 统计日期
     * @param userId 用户唯一标识（用户ID/设备ID/IP，随便填一个能区分用户的）
     */
    public void addUV(LocalDate date, String userId) {
        String uvKey = "uv:" + date.format(DATE_FORMAT); // 例：uv:20260408
        stringRedisTemplate.opsForHyperLogLog().add(uvKey, userId); // 自动去重
        stringRedisTemplate.expire(uvKey, 7, java.util.concurrent.TimeUnit.DAYS);
    }

    /**
     * 查询UV（直接传日期就能查）
     */
    public Long getUV(LocalDate date) {
        String uvKey = "uv:" + date.format(DATE_FORMAT);
        return stringRedisTemplate.opsForHyperLogLog().size(uvKey);
    }

}

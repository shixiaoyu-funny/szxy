package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.PositionDTO;
import com.shixiaoyu.xiangyueproject.entity.vo.PositionVO;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 省市区经纬度缓存实现。
 * Redis key：pos:省名:市名:区名
 * Redis value：经度,纬度（String，永久缓存）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PositionServiceImpl implements PositionService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PositionVO getPos(String province, String city, String county) {
        // 校验省市区非空，避免生成脏 key
        requireRegion(province, city, county);
        String key = buildKey(province, city, county);
        String cached = stringRedisTemplate.opsForValue().get(key);
        PositionVO vo = new PositionVO();
        vo.setProvince(province.trim());
        vo.setCity(city.trim());
        vo.setCounty(county.trim());
        if (StrUtil.isBlank(cached)) {
            // 未命中：经纬度返回 null，由前端调高德地理编码
            log.debug("经纬度缓存未命中 key={}", key);
            vo.setLongitude(null);
            vo.setLatitude(null);
            return vo;
        }
        // 命中：解析 "经度,纬度"
        String[] parts = cached.split(",", 2);
        if (parts.length != 2 || StrUtil.isBlank(parts[0]) || StrUtil.isBlank(parts[1])) {
            log.warn("经纬度缓存格式异常 key={} value={}", key, cached);
            vo.setLongitude(null);
            vo.setLatitude(null);
            return vo;
        }
        vo.setLongitude(parts[0].trim());
        vo.setLatitude(parts[1].trim());
        log.debug("经纬度缓存命中 key={} location={}", key, cached);
        return vo;
    }

    @Override
    public void savePos(PositionDTO dto) {
        if (dto == null) {
            throw new BusinessException("位置参数不能为空");
        }
        requireRegion(dto.getProvince(), dto.getCity(), dto.getCounty());
        if (StrUtil.isBlank(dto.getLongitude()) || StrUtil.isBlank(dto.getLatitude())) {
            throw new BusinessException("经纬度不能为空");
        }
        String key = buildKey(dto.getProvince(), dto.getCity(), dto.getCounty());
        // 与高德 geocodes[0].location 格式一致：经度,纬度
        String value = dto.getLongitude().trim() + "," + dto.getLatitude().trim();
        // 不设 TTL：同一省市区坐标稳定，长期缓存即可
        stringRedisTemplate.opsForValue().set(key, value);
        log.info("经纬度已写入 Redis key={} value={}", key, value);
    }

    /** 拼装 Redis key：pos:省:市:区 */
    private String buildKey(String province, String city, String county) {
        return RedisConstants.POS_PREFIX
                + province.trim() + ":"
                + city.trim() + ":"
                + county.trim();
    }

    private void requireRegion(String province, String city, String county) {
        if (StrUtil.isBlank(province) || StrUtil.isBlank(city) || StrUtil.isBlank(county)) {
            throw new BusinessException("省、市、区均不能为空");
        }
    }
}

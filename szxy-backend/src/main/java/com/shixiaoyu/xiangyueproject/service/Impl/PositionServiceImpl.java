package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.PosReportDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.PositionDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.vo.PositionVO;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.service.PositionService;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 省市区经纬度缓存 + 用户上次定位比对。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PositionServiceImpl implements PositionService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public PositionVO getPos(String province, String city, String county) {
        requireRegion(province, city, county);
        String key = buildKey(province, city, county);
        String cached = stringRedisTemplate.opsForValue().get(key);
        PositionVO vo = new PositionVO();
        vo.setProvince(province.trim());
        vo.setCity(city.trim());
        vo.setCounty(county.trim());
        if (StrUtil.isBlank(cached)) {
            log.debug("经纬度缓存未命中 key={}", key);
            vo.setLongitude(null);
            vo.setLatitude(null);
            return vo;
        }
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
        String value = dto.getLongitude().trim() + "," + dto.getLatitude().trim();
        stringRedisTemplate.opsForValue().set(key, value);
        log.info("经纬度已写入 Redis key={} value={}", key, value);
    }

    @Override
    public void reportPos(PosReportDTO dto) {
        if (dto == null) {
            throw new BusinessException("位置参数不能为空");
        }
        requireRegion(dto.getProvince(), dto.getCity(), dto.getCounty());
        Long userId = UserHolder.getUser().getId();
        String province = dto.getProvince().trim();
        String city = dto.getCity().trim();
        String county = dto.getCounty().trim();
        // 未知区域 / 局域网：不写 Redis、不发推荐
        if (isUnknownRegion(province, city, county)) {
            log.info("忽略未知区域上报 uid={} {}|{}|{}", userId, province, city, county);
            return;
        }
        String newVal = province + "|" + city + "|" + county;
        String redisKey = RedisConstants.USER_LAST_POS_PREFIX + userId;
        String oldVal = stringRedisTemplate.opsForValue().get(redisKey);

        // 无历史：只写入，不发 MQ
        if (StrUtil.isBlank(oldVal)) {
            stringRedisTemplate.opsForValue().set(redisKey, newVal);
            log.info("首次记录用户定位 uid={} pos={}", userId, newVal);
            return;
        }
        // 相同：不更新、不发
        if (newVal.equals(oldVal.trim())) {
            log.debug("用户定位未变化 uid={} pos={}", userId, newVal);
            return;
        }

        stringRedisTemplate.opsForValue().set(redisKey, newVal);
        log.info("用户定位已变更 uid={} {} -> {}", userId, oldVal, newVal);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        if (!Integer.valueOf(1).equals(user.getOpenPosAlter())) {
            log.debug("用户未开启位置推荐 uid={}", userId);
            return;
        }
        if (StrUtil.isBlank(user.getEmail())) {
            log.warn("用户开启了位置推荐但未绑定邮箱 uid={}", userId);
            return;
        }

        Map<String, String> map = new HashMap<>();
        map.put("uid", userId.toString());
        map.put("email", user.getEmail().trim());
        map.put("province", province);
        map.put("city", city);
        map.put("county", county);
        rabbitTemplate.convertAndSend(
                CommonConstants.EXCHANGE_NAME,
                CommonConstants.POS_ALTER_ROUTING_KEY,
                map);
        log.info("已投递位置变更推荐消息 uid={} email={}", userId, user.getEmail());
    }

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

    /** 未知区域、局域网等无效定位，不上报也不发邮件 */
    private boolean isUnknownRegion(String province, String city, String county) {
        return isUnknownToken(province) || isUnknownToken(city) || isUnknownToken(county);
    }

    private boolean isUnknownToken(String value) {
        if (StrUtil.isBlank(value)) {
            return true;
        }
        String v = value.trim();
        return "未知区域".equals(v)
                || "未知位置".equals(v)
                || "局域网".equals(v)
                || v.contains("未知");
    }
}

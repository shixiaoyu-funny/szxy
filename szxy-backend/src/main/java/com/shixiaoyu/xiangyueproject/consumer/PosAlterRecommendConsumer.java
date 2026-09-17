package com.shixiaoyu.xiangyueproject.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.entity.po.ShopProduct;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.enums.ProductTypeEnum;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.ShopProductMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户定位变更后：查当地村/景点/特产 → Agent 生成推荐 → 邮件发送。
 */
@Slf4j
@Component
@RabbitListener(bindings = @QueueBinding(
        value = @Queue(name = CommonConstants.POS_ALTER_QUEUE_NAME),
        exchange = @Exchange(name = CommonConstants.EXCHANGE_NAME),
        key = CommonConstants.POS_ALTER_ROUTING_KEY))
public class PosAlterRecommendConsumer {

    private static final int MAX_VILLAGE = 8;
    private static final int MAX_SCENIC = 12;
    private static final int MAX_PRODUCT = 12;

    @Resource
    private UserMapper userMapper;
    @Resource
    private VillageMapper villageMapper;
    @Resource
    private ScenicMapper scenicMapper;
    @Resource
    private ShopProductMapper shopProductMapper;
    @Resource
    private ChatClient qwenFlashClient;
    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @RabbitHandler
    public void onPosAlter(Map<String, String> data) {
        if (data == null) {
            return;
        }
        String uidStr = data.get("uid");
        String email = StrUtil.trim(data.get("email"));
        String province = StrUtil.trim(data.get("province"));
        String city = StrUtil.trim(data.get("city"));
        String county = StrUtil.trim(data.get("county"));
        if (StrUtil.isBlank(uidStr) || StrUtil.isBlank(email)
                || StrUtil.isBlank(province) || StrUtil.isBlank(city) || StrUtil.isBlank(county)) {
            log.warn("位置推荐消息字段不完整: {}", data);
            return;
        }

        Long userId;
        try {
            userId = Long.parseLong(uidStr);
        } catch (NumberFormatException e) {
            log.warn("位置推荐消息 uid 非法: {}", uidStr);
            return;
        }

        try {
            User user = userMapper.selectById(userId);
            if (user == null || !Integer.valueOf(1).equals(user.getOpenPosAlter())) {
                log.info("跳过位置推荐：用户不存在或已关闭开关 uid={}", userId);
                return;
            }
            String to = StrUtil.blankToDefault(StrUtil.trim(user.getEmail()), email);
            if (StrUtil.isBlank(to)) {
                log.warn("跳过位置推荐：无邮箱 uid={}", userId);
                return;
            }

            String dbDigest = buildDbDigest(province, city, county);
            String regionLabel = province + city + county;
            String prompt = """
                    用户当前位置变更为：%s。
                    请根据以下平台内真实数据，用亲切简洁的中文写一封推荐短文（适合纯文本邮件）：
                    1. 推荐当地值得去的景点（若有）
                    2. 推荐当地特产/可购商品（若有）
                    3. 没有数据时如实说明，不要编造具体村名/景点/价格
                    4. 不要使用 Markdown 代码块，少用特殊符号
                    
                    【平台数据摘要】
                    %s
                    """.formatted(regionLabel, dbDigest);

            String body = qwenFlashClient.prompt().user(prompt).call().content();
            if (StrUtil.isBlank(body)) {
                body = "您好，已检测到您当前位置为「" + regionLabel + "」。\n\n" + dbDigest;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject("数智乡约 · 您所在地的景点和特产推荐");
            message.setText(body.trim());
            mailSender.send(message);
            log.info("位置推荐邮件已发送 uid={} email={} region={}", userId, to, regionLabel);
        } catch (Exception e) {
            log.error("位置推荐处理失败 uid={} region={}-{}-{}", userId, province, city, county, e);
        }
    }

    private String buildDbDigest(String province, String city, String county) {
        List<VillageBase> villages = villageMapper.selectList(new LambdaQueryWrapper<VillageBase>()
                .eq(VillageBase::getProvince, province)
                .eq(VillageBase::getCity, city)
                .eq(VillageBase::getCounty, county)
                .last("LIMIT " + MAX_VILLAGE));
        // 区县精确无结果时，放宽到同市
        if (villages.isEmpty()) {
            villages = villageMapper.selectList(new LambdaQueryWrapper<VillageBase>()
                    .eq(VillageBase::getProvince, province)
                    .eq(VillageBase::getCity, city)
                    .last("LIMIT " + MAX_VILLAGE));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("定位：").append(province).append(city).append(county).append('\n');
        if (villages.isEmpty()) {
            sb.append("村落：暂无匹配数据\n景点：暂无\n特产/商品：暂无\n");
            return sb.toString();
        }

        sb.append("村落：\n");
        for (VillageBase v : villages) {
            sb.append("- ").append(v.getName());
            if (StrUtil.isNotBlank(v.getIntro())) {
                sb.append("：").append(StrUtil.sub(v.getIntro(), 0, 80));
            }
            sb.append('\n');
        }

        List<Long> villageIds = villages.stream().map(VillageBase::getId).toList();
        List<VillageScenic> scenics = scenicMapper.selectList(new LambdaQueryWrapper<VillageScenic>()
                .in(VillageScenic::getVillageId, villageIds)
                .last("LIMIT " + MAX_SCENIC));
        sb.append("景点：\n");
        if (CollUtil.isEmpty(scenics)) {
            sb.append("- 暂无\n");
        } else {
            Map<Long, String> villageName = villages.stream()
                    .collect(Collectors.toMap(VillageBase::getId, VillageBase::getName, (a, b) -> a));
            for (VillageScenic s : scenics) {
                sb.append("- ").append(s.getName());
                String vn = villageName.get(s.getVillageId());
                if (StrUtil.isNotBlank(vn)) {
                    sb.append("（").append(vn).append("）");
                }
                if (StrUtil.isNotBlank(s.getIntro())) {
                    sb.append("：").append(StrUtil.sub(s.getIntro(), 0, 60));
                }
                sb.append('\n');
            }
        }

        List<ShopProduct> products = shopProductMapper.selectList(new LambdaQueryWrapper<ShopProduct>()
                .in(ShopProduct::getVillageId, villageIds)
                .eq(ShopProduct::getStatus, 1)
                .orderByAsc(ShopProduct::getType)
                .last("LIMIT " + MAX_PRODUCT));
        sb.append("特产/商品：\n");
        if (CollUtil.isEmpty(products)) {
            sb.append("- 暂无\n");
        } else {
            for (ShopProduct p : products) {
                String typeLabel = productTypeLabel(p.getType());
                sb.append("- [").append(typeLabel).append("] ").append(p.getName());
                if (p.getPrice() != null) {
                    sb.append(" ¥").append(p.getPrice());
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String productTypeLabel(ProductTypeEnum type) {
        if (type == null) {
            return "商品";
        }
        return switch (type) {
            case PHYSICAL -> "特产";
            case TICKET -> "门票";
            case STAY -> "住宿";
        };
    }
}

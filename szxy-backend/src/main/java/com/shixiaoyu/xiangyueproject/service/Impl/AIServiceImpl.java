package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixiaoyu.xiangyueproject.entity.dto.AIDTO;
import com.shixiaoyu.xiangyueproject.entity.po.AITripRecommend;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.result.ExternalMultiResult;
import com.shixiaoyu.xiangyueproject.entity.result.InnerMultiResult;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.AIRecommendVO;
import com.shixiaoyu.xiangyueproject.mapper.AIMapper;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.service.AIService;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import com.shixiaoyu.xiangyueproject.util.CacheUtils;
import com.shixiaoyu.xiangyueproject.util.RecognizeUtils;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.AI_ALLOWED_CACHE_CONTENT;
import static com.shixiaoyu.xiangyueproject.constants.CommonConstants.OSS_PREFIX;
import static com.shixiaoyu.xiangyueproject.constants.ErrorConstants.JSON_ERROR;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.*;
import static com.shixiaoyu.xiangyueproject.enums.AIGenerateEnum.AI_RECOMMEND;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl extends ServiceImpl<AIMapper, AITripRecommend> implements AIService {
    private final ChatClient villageClient;
    private final ChatClient scenicClient;
    private final ChatClient specialityClient;
    private final ChatClient commonChatClient;
    private final ChatClient multiClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final VillageService villageService;
    private final RecognizeUtils recognizeUtils;
    private final ObjectMapper objectMapper;
    private final ScenicMapper scenicMapper;

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            2,
            5,
            30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(5),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Override
    public Result<AIRecommendVO> recommendItinerary(AIDTO aidto) {
        // 获取content，处理null
        String userContent = aidto.getContent() == null ? "" : aidto.getContent().trim();
        String key = null;

        // 白名单话术走缓存查询
        if (AI_ALLOWED_CACHE_CONTENT.contains(userContent)) {
            key = AI_RECOMMEND_PREFIX + CacheUtils.getAiCacheKey(aidto);
            String voString = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(voString)) {
                AIRecommendVO recommendVO = JSONUtil.toBean(voString, AIRecommendVO.class);
                if (recommendVO != null) {
                    return Result.ok(recommendVO);
                }
            }
        }
        //---获取推荐农村ids;
        List<VillageBase> villageBaseList = villageService.list();
        String userPrompt = String.format("""
                用户请求参数：%s
                候选村落列表：%s
                """, JSONUtil.toJsonStr(aidto), JSONUtil.toJsonStr(villageBaseList));
        String villageIdArray = villageClient.prompt()
                .user(userPrompt)
                .call()
                .content();
        log.info("villageIdArray:{}", villageIdArray);
        if(villageIdArray == null || villageIdArray.equals("[]")){
            return Result.ok(new AIRecommendVO(new ArrayList<>(), "没有推荐结果", AI_RECOMMEND.getCode()));
        }
        List<Long> villageIdList = getLongIds(villageIdArray);
        if (CollectionUtils.isEmpty(villageIdList)) {
            return Result.ok(new AIRecommendVO(new ArrayList<>(), "没有推荐结果", AI_RECOMMEND.getCode()));
        }
        //---获取ids对应的农村对象
        List<VillageBase> villageBases = villageService.listByIds(villageIdList);
        log.info("villageBases:{}", villageBases);
        //根据ids查询所有景点
        LambdaQueryWrapper<VillageScenic> wrapper = new LambdaQueryWrapper<>();
        List<VillageScenic> allScenics = scenicMapper.selectList(wrapper.in(VillageScenic::getVillageId, villageIdList));
        log.info("allScenics:{}", allScenics);
        //构建景点AI提示词
        String userPrompt1 = String.format("""
                        tripPeople：%d
                        money：%d
                        content：%s
                        景点JSON列表：%s
                        """,
                // 处理null值，避免AI解析出错
                aidto.getTripPeople() == null ? 0 : aidto.getTripPeople(),
                aidto.getMoney() == null ? 0 : aidto.getMoney(),
                aidto.getContent() == null ? "" : aidto.getContent(),
                JSONUtil.toJsonStr(allScenics)
        );
        //整合AI推荐的农村及其下属景点列表信息
        CopyOnWriteArrayList<Map<String, List<VillageScenic>>> recommendList = new CopyOnWriteArrayList<>();
        AtomicReference<String> content = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);
        CopyOnWriteArrayList<Map<String, List<VillageScenic>>> finalRecommendList = recommendList;
        threadPoolExecutor.execute(() -> {
            try {
                //传给景点AI获取推荐景点ids
                String scenicIdArray = scenicClient.prompt()
                        .user(userPrompt1)
                        .call()
                        .content();
                log.info("scenicIdArray:{}", scenicIdArray);
                List<Long> scenicIdList = getLongIds(scenicIdArray);
                //根据推荐景点ids查询景点具体信息
                LambdaQueryWrapper<VillageScenic> asyncWrapper  = new LambdaQueryWrapper<>();
                List<VillageScenic> scenics = scenicMapper.selectList(asyncWrapper.in(VillageScenic::getVillageId, villageIdList));
                log.info("scenics:{}", scenics);
                for (VillageBase village : villageBases) {
                    Map<String, List<VillageScenic>> map = new HashMap<>();
                    List<VillageScenic> partScenics = new ArrayList<>();
                    for (VillageScenic scenic : scenics) {
                        if (scenic.getVillageId().equals(village.getId())) {
                            partScenics.add(scenic);
                        }
                    }
                    map.put(village.getName(), partScenics);
                    finalRecommendList.add(map);
                }
                log.info("recommendList:{}", finalRecommendList);
            } catch (Exception e) {
                log.error("景点AI异步任务执行失败", e);
            } finally {
                log.info("景点AI异步任务执行完成");
                latch.countDown();//任务完成
            }
        });
        threadPoolExecutor.execute(() -> {
            try {
                String specialtyPrompt = "农村JSON列表：" + JSONUtil.toJsonStr(villageBases);
                //---AI根据选定的农村信息（JSON结构），通过网络大数据搜索推荐相关特产信息
                content.set(specialityClient.prompt()
                        .user(specialtyPrompt)
                        .call()
                        .content());
                log.info("content:{}", content);
            } catch (Exception e) {
                log.error("特产AI异步任务执行失败", e);
            } finally {
                log.info("特产AI异步任务执行完成");
                latch.countDown();//任务完成
            }
        });
        //主线程等待任务完成
        try {
            boolean await = latch.await(70, TimeUnit.SECONDS);
            if (!await) {
                log.warn("任务超时");
                content.set("特产信息获取超时，请稍后重试");
            }
        } catch (InterruptedException e) {
            log.error("主线程等待被中断", e);
            Thread.currentThread().interrupt(); // 恢复中断状态
        }
        //组装返回结果
        AIRecommendVO aiRecommendVO = new AIRecommendVO(new ArrayList<>(recommendList), content.get(), AI_RECOMMEND.getCode());
        log.info("aiRecommendVO:{}", aiRecommendVO);
        //缓存结果
        if (StringUtils.isNotBlank(key)) {
            Boolean redisRes = stringRedisTemplate.opsForValue().setIfAbsent(
                    key,
                    JSONUtil.toJsonStr(aiRecommendVO),
                    AI_RECOMMEND_TTL,
                    TimeUnit.HOURS);
            if (!redisRes) {
                log.warn("本次记录缓存失败，对象为：{}", aiRecommendVO);
            }
        }
        return Result.ok(aiRecommendVO);
    }

    @Override
    public Result<String> inquire(String content) {
        if (content == null) {
            return Result.error("请输入查询内容");
        }
        String res = commonChatClient.prompt()
                .user(content)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, UserHolder.getUser().getId()))
                .call()
                .content();
        log.info("res:{}", res);
        String key=AI_MEMORY+UserHolder.getUser().getId().toString();
        stringRedisTemplate.opsForList().leftPushAll(key,content,res);
        stringRedisTemplate.expire(key,AI_MEMORY_TTL,TimeUnit.HOURS);
        return Result.ok(res);
    }

    @Override
    public Result<String> multimodal(String imageUrl) {
        if (!imageUrl.startsWith(OSS_PREFIX)) {
            return Result.error("图片url错误");
        }
        String recognizeRes = recognizeUtils.recognize(imageUrl);
        ExternalMultiResult externalMultiResult= new ExternalMultiResult();
        try {
            externalMultiResult= objectMapper.readValue(recognizeRes, ExternalMultiResult.class);
        } catch (JsonProcessingException e) {
            log.error(JSON_ERROR);
        }
        log.info("externalMultiResult:{}", externalMultiResult);
        if(externalMultiResult==null || externalMultiResult.getResultNum()==0){
            Result.ok("未识别出相关信息，请换个角度拍摄看看呢~");
        }
        int resultNum = externalMultiResult.getResultNum();
        List<InnerMultiResult> innerMultiResults = externalMultiResult.getResult();
        innerMultiResults.sort(Comparator.comparingDouble(InnerMultiResult::getScore).reversed());
        InnerMultiResult res = innerMultiResults.get(0);
        String key=AI_MULTI_PREFIX+res.getName();
        String preContent = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.isNotBlank(preContent)){
            return Result.ok(preContent);
        }
        String content = multiClient.prompt()
                .user(res.getName())
                .call()
                .content();
        log.info("content:{}", content);
        //将结果缓存到redis中
        Boolean redisRes = stringRedisTemplate.opsForValue().setIfAbsent(key, content, AI_MULTI_TTL, TimeUnit.HOURS);
        if(!redisRes){
            log.warn("本次记录缓存失败，对象为：{}",res.getName());
        }
        return Result.ok(content);
    }

    @Override
    public Result<List<String>> memory() {
        String key=AI_MEMORY+UserHolder.getUser().getId().toString();
        List<String> list = stringRedisTemplate.opsForList().range(key, 0, -1);
        Collections.reverse(list);
        return Result.ok(list);
    }

    private List<Long> getLongIds(String idArray) {
        // 兜底：空值/空数组直接返回空列表
        if (idArray == null || idArray.isBlank() || "[]".equals(idArray)) {
            return new ArrayList<>();
        }
        idArray = idArray.replace(" ", "");
        String[] splitRes = idArray.substring(1, idArray.length() - 1).split(",");
        log.info("splitRes:{}", Arrays.toString(splitRes));
        if (splitRes.length == 0) {
            return new ArrayList<>();
        }
        long[] ids = Arrays.stream(splitRes).mapToLong(Long::parseLong).toArray();
        List<Long> idList = new ArrayList<>();
        for (long id : ids) {
            idList.add(id);
        }
        if (ids.length == 0) {
            return null;
        }
        return idList;
    }
}




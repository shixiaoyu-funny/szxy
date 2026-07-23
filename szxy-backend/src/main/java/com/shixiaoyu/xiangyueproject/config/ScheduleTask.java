package com.shixiaoyu.xiangyueproject.config;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleTask {
    private final VillageService villageService;
    private final ScenicMapper scenicMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestHighLevelClient restHighLevelClient;
    // ES索引名（和你Kibana创建的一致）
    private static final String ES_INDEX = "village";
    // 每页查询数量（防OOM，推荐500-1000）
    private static final int PAGE_SIZE = 1000;
    /**
     * 更新点赞排行榜的定时任务
     */
    @Scheduled(cron = "0/15 * * * * ? ")
    public void syncVillageLikeRank(){
        //查询全部农村数据
        List<VillageBase> villageBaseList = villageService.list();
        if(CollectionUtils.isEmpty(villageBaseList)){
            return;
        }
        List<Long> idList = villageBaseList.stream().map(VillageBase::getId).toList();
        //根据农村id查询点赞数（得到的list里面有两个map，一个是id的，一个是total_likes的）
        List<Map<String,Object>> resultList =scenicMapper.selectLikes(idList);
        //解析数据
        Map<Long, Integer> villageLikeMap = resultList.stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("id")).longValue(),
                        map -> ((Number) map.get("total_likes")).intValue()
                ));
        stringRedisTemplate.delete(VILLAGE_LIKES_TOP_10);
        for (VillageBase village : villageBaseList) {
            Long villageId = village.getId();
            int totalLikes = villageLikeMap.getOrDefault(villageId, 0);
            // ZSet: key=排行key, value=农村id, score=总点赞数
            stringRedisTemplate.opsForZSet().add(VILLAGE_LIKES_TOP_10, villageId.toString(), totalLikes);
        }
        // 设置过期时间
        stringRedisTemplate.expire(VILLAGE_LIKES_TOP_10, VILLAGE_TOP_10_TTL, TimeUnit.HOURS);
    }

    /**
     * 更新收藏排行榜的定时任务
     */
    @Scheduled(cron = "0/15 * * * * ? ")
    public void syncVillageCollectionsRank(){
        //查询全部数据
        List<VillageBase> villageBaseList = villageService.list();
        List<Long> idList = villageBaseList.stream().map(VillageBase::getId).toList();
        List<Map<String, Object>> maps = scenicMapper.selectCollections(idList);
        //数据解析
        Map<Long, Integer> solveMap = maps.stream().collect(
                Collectors.toMap(
                        map -> ((Number) map.get("id")).longValue(),
                        map -> ((Number) map.get("total_collections")).intValue()
                )
        );
        //清除缓存
        stringRedisTemplate.delete(VILLAGE_COLLECTIONS_TOP_10);
        //缓存重建
        for(Long id:idList){
            Integer collections = solveMap.get(id);
            stringRedisTemplate.opsForZSet().add(VILLAGE_COLLECTIONS_TOP_10,id.toString(),collections);
        }
    }
    /**
     * 定时更新es数据(15分钟更新一次)
     */
    @Scheduled(cron = "0 0/15 * * * ?")
    public void updateEsData() {
        log.info("===== 删除ES下所有数据 =====");
        try {
            clearAllEsData();
        } catch (IOException e) {
            log.info("没有可删除的数据，正在重建es缓存...");
            try {
                syncEsData();
            } catch (IOException ex) {
                log.error("更新失败！！！");
            }
            return;
        }
        try {
            syncEsData();
        } catch (IOException e) {
            log.error("更新失败！！！");
        }
    }
//    @Scheduled(cron = "0/15 * * * * ?")
//    public void updateEsData() {
//        log.info("===== 删除ES下所有数据 =====");
//        try {
//            clearAllEsData();
//        } catch (IOException e) {
//            log.info("没有可删除的数据，正在重建es缓存...");
//            try {
//                syncEsData();
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
//            return;
//        }
//        try {
//            syncEsData();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private void syncEsData() throws IOException {
        log.info("===== 开始定时同步MySQL村落数据到ES =====");

        int total = 0;
        int currentPage = 1;

        try {
            // 分页循环同步（全量更新）
            while (true) {
                Page<VillageBase> page = new Page<>(currentPage, PAGE_SIZE);
                Page<VillageBase> resultPage = villageService.page(page);
                List<VillageBase> villageList = resultPage.getRecords();

                // 没有数据，退出循环
                if (villageList.isEmpty()) {
                    break;
                }

                // 批量插入ES
                insertBatchDoc(villageList);
                total += villageList.size();
                currentPage++;
            }

            log.info("===== ES数据定时同步完成，总同步数据量：{} =====", total);
        } catch (Exception e) {
            log.error("===== ES数据定时同步失败 =====", e);
        }
    }

    /**
     * 批量插入ES文档
     */
    private void insertBatchDoc(List<VillageBase> villageList) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();

        for (VillageBase village : villageList) {
            // 构建ES文档：id=数据库id，数据转JSON
            bulkRequest.add(new IndexRequest(ES_INDEX)
                    .id(village.getId().toString()) // 用数据库id作为ES唯一id，重复会覆盖更新
                    .source(JSONUtil.toJsonStr(village), XContentType.JSON));
        }

        // 执行批量插入
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    /**
     * 清空ES索引下的所有数据
     */
    private void clearAllEsData() throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(ES_INDEX);
        request.setQuery(QueryBuilders.matchAllQuery()); // 删除所有
        restHighLevelClient.deleteByQuery(request, RequestOptions.DEFAULT);
    }
}

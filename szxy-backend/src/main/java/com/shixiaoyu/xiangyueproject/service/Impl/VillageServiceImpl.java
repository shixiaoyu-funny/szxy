package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.VillageBaseDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.VillageSortDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.ScenicService;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.shixiaoyu.xiangyueproject.constants.ErrorConstants.*;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.*;

@Service
@RequiredArgsConstructor
public class VillageServiceImpl extends ServiceImpl<VillageMapper, VillageBase> implements VillageService {
    private final VillageMapper villageMapper;
    private final ScenicMapper scenicMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final FarmerMapper farmerMapper;

    @Override
    public Result<PageResultVO<VillageBaseVO>> villageList(PageResultDTO pageResultDTO) {
        int pageNo = pageResultDTO.getPageNo();
        int pageSize = pageResultDTO.getPageSize();
        Page p = Page.of(pageNo, pageSize);
        p.addOrder(OrderItem.asc("create_time"));
        Page<VillageBase> page = page(p);
        List<VillageBaseVO> voList = page.getRecords().stream().map(po -> BeanUtil.copyProperties(po, VillageBaseVO.class)).toList();
        return Result.ok(new PageResultVO(page.getTotal(), voList));
    }

    @Override
    public Result addVillage(VillageBaseDTO villageBaseDTO) {
        if (villageBaseDTO == null) {
            return Result.error(INSERT_NULL);
        }
        int insert = villageMapper.insert(BeanUtil.copyProperties(villageBaseDTO, VillageBase.class));
        if (insert == 0) {
            return Result.error(ERROR_INSERT);
        }
        return Result.ok();
    }

    @Override
    public Result updateVillage(Long id, VillageBaseDTO villageBaseDTO) {
        if (id == null) {
            return Result.error(NULL_ID);
        }
        VillageBase villageBase = getById(id);
        if (villageBase == null) {
            return Result.error(DATA_NOT_EXIST);
        }
        boolean update = lambdaUpdate().eq(VillageBase::getId, id).update(BeanUtil.copyProperties(villageBaseDTO, VillageBase.class));
        if (!update) {
            return Result.error(ERROR_UPDATE);
        }
        return Result.ok();
    }

    @Override
    public Result<List<VillageBaseVO>> villageLikes() {
        //补充点赞量
        return Result.ok(getSorted(VILLAGE_LIKES_TOP_10));
    }

    @Override
    public Result<List<VillageBaseVO>> villageCollections() {
        return Result.ok(getSorted(VILLAGE_COLLECTIONS_TOP_10));
    }

    /**
     * 收藏点赞通用排行
     * @param key
     * @return
     */
    private List<VillageBaseVO> getSorted(String key){
        //查缓存
        Set<ZSetOperations.TypedTuple<String>> set = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 9);
        if(CollectionUtils.isEmpty(set)){
            return Collections.emptyList();
        }
        //解析数据
        List<Long> idList=new ArrayList<>();
        Map<Long,Integer> map=new HashMap<>();
        for(ZSetOperations.TypedTuple<String> ele:set){
            long id = Long.parseLong(ele.getValue());
            int scores = ele.getScore().intValue();
            idList.add(id);
            map.put(id,scores);
        }
        //有序查询
        String idStr = StrUtil.join(",", idList);
        List<VillageBase> sortedList = query().in("id", idList)
                .last("order by field(id," + idStr + ")")
                .list();
        List<VillageBaseVO> voList = sortedList.stream()
                .map(vb->{
                    VillageBaseVO villageBaseVO = BeanUtil.copyProperties(vb, VillageBaseVO.class);
                    Long manageId = vb.getManageId();
                    if(manageId==null){
                        LambdaQueryWrapper<FarmerUser> wrapper = new LambdaQueryWrapper<FarmerUser>().eq(FarmerUser::getVillageId, vb.getId());
                        FarmerUser farmerUser = farmerMapper.selectOne(wrapper);
                        villageBaseVO = BeanUtil.copyProperties(vb, VillageBaseVO.class);
                        if (farmerUser!=null) {
                            villageBaseVO.setManagerName(farmerUser.getFarmName());
                        }
                    }
                    else{
                        LambdaQueryWrapper<FarmerUser> wrapper = new LambdaQueryWrapper<FarmerUser>().eq(FarmerUser::getUserId, vb.getManageId());
                        FarmerUser farmerUser = farmerMapper.selectOne(wrapper);
                        if(farmerUser!=null){
                            villageBaseVO.setManagerName(farmerUser.getFarmName());
                        }
                    }
                    return villageBaseVO;
                })
                .toList();
        //补充分数
        for(VillageBaseVO vo:voList){
            if (key.equals(VILLAGE_COLLECTIONS_TOP_10)) {
                vo.setCollects(map.get(vo.getId()));
            } else {
                vo.setLikes(map.get(vo.getId()));
            }

        }
        return voList;
    }
}

package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.AIDTO;
import com.shixiaoyu.xiangyueproject.entity.po.AITripRecommend;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.AIRecommendVO;

import java.util.List;

public interface AIService extends IService<AITripRecommend> {
    Result<AIRecommendVO> recommendItinerary(AIDTO aidto);

    Result<String> inquire(String content);

    Result<String> multimodal(String imageUrl);

    Result<List<String>> memory();
}

package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.entity.dto.AIDTO;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.AIRecommendVO;
import com.shixiaoyu.xiangyueproject.service.AIService;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/common/ai")
@Tag(name = "AI接口（通用）")
@RequiredArgsConstructor
public class AIController {
    private final AIService aiService;
    /**
     * AI智能推荐
     * @param AIDTO
     * @return
     */
    @PostMapping("/recommend")
    @Operation(summary = "AI智能推荐")
    public Result<AIRecommendVO> recommendItinerary(@Parameter(name = "smartItineraryDTO",required = true) @RequestBody AIDTO AIDTO
    ){
        log.info("AIDTO:{}",AIDTO);
        return aiService.recommendItinerary(AIDTO);
    }

    /**
     * 智能问答
     * @param content
     * @return
     */
    @PostMapping("/inquire")
    @Operation(summary = "AI智能问答")
    public Result<String> inquire(@Parameter(description = "内容",name = "content",required = true) @RequestParam String content){
        return aiService.inquire(content);
    }

    @GetMapping("/memory")
    @Operation(summary = "AI会话记忆")
    public Result<List<String>> memory(){
        log.info("正在加载id为{}的对话...", UserHolder.getUser().getId());
        return aiService.memory();
    }

    /**
     * 多模态识别农产品
     * @param imageUrl
     * @return
     */
    @PostMapping("/multimodal")
    @Operation(summary = "多模态识别农产品")
    public Result<String> multimodal(@Parameter(description = "图片url",name = "imageUrl",required = true) @RequestParam String imageUrl){
        return aiService.multimodal(imageUrl);
    }
}

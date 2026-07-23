package com.shixiaoyu.xiangyueproject.controller;

import cn.hutool.core.bean.BeanUtil;
import com.shixiaoyu.xiangyueproject.entity.dto.ScenicAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.po.UserComment;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserCommentVO;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.service.ScenicService;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/scenic")
@Tag(name = "景点接口")
@RequiredArgsConstructor
public class ScenicController {
    private final ScenicService scenicService;
    private final ScenicMapper scenicMapper;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 农户注册私人景点
     * @param scenicDTO
     * @return
     */
    @PostMapping("/private_scenic")
    @Operation(summary = "注册景点")
    public Result registerPrivateScenic(@Parameter(description = "注册景点信息",name = "scenicDTO",required = true) @RequestBody ScenicAccessDTO scenicDTO){
        LocalDate now = LocalDate.now();
        stringRedisTemplate.opsForHyperLogLog().add("new:village"+now.format(DATE_FORMAT),scenicDTO.getName());
        scenicDTO.setUserId(UserHolder.getUser().getId());
        return scenicService.registerPrivateScenic(scenicDTO);
    }

    /**
     * 村长注册公共旅游景点
     * @param scenicDTO
     * @return
     */
    @PostMapping("/public_scenic")
    @Operation(summary = "注册景点")
    public Result registerPublicScenic(@Parameter(description = "注册景点信息",name = "scenicDTO",required = true) @RequestBody ScenicAccessDTO scenicDTO){
        LocalDate now = LocalDate.now();
        stringRedisTemplate.opsForHyperLogLog().add("new:village"+now.format(DATE_FORMAT),scenicDTO.getName());
        scenicDTO.setUserId(UserHolder.getUser().getId());
        return scenicService.registerPublicScenic(scenicDTO);
    }

    /**
     * 查询全部景点注册申请表
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询全部景点注册申请表")
    public Result list(){
        return scenicService.getScenicAccessList();
    }

    /**
     * 查看优质景点信息（top10，根据点赞量）
     * @return
     */
    @GetMapping("/scenic")
    @Operation(summary = "查看优质景点信息（top10，根据点赞量）")
    public Result<List<ScenicVO>> scenic(){
        return scenicService.getTop10Scenic();
    }

    /**
     * 查看景点详细信息
     * @param id
     * @return
     */
    @GetMapping("/detail")
    @Operation(summary = "查看景点详情")
    public Result<ScenicVO> detail(@Parameter(description = "景点id",name = "id",required = true) @RequestParam Long id){
        return scenicService.detail(id);
    }

    /**
     * 查看景点评论
     * @param id
     * @return
     */
    @GetMapping("/sc_comments")
    @Operation(summary = "查看景点评论")
    public Result<List<UserCommentVO>> scComments(@Parameter(description = "景点id",name = "id",required = true) @RequestParam Long id){
        return Result.ok(scenicService.getScComments(id));
    }
}

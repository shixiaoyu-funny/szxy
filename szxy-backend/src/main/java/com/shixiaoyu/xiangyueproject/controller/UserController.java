package com.shixiaoyu.xiangyueproject.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.UserCommentDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.UserSetInfoDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerAccess;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.LocationVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.mapper.FarmerAccessMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.server.WebSocketServer;
import com.shixiaoyu.xiangyueproject.service.LoginService;
import com.shixiaoyu.xiangyueproject.service.UserService;
import com.shixiaoyu.xiangyueproject.util.ClientUtils;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户接口")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final VillageMapper villageMapper;
    private final FarmerAccessMapper farmerAccessMapper;
    private final WebSocketServer webSocketServer;

    @GetMapping("/info")
    @Operation(summary = "获取用户信息")
    public Result<UserVO> info(){
        User user = userService.getById(UserHolder.getUser().getId());
        log.info("用户:{} 获取用户信息成功", user.getId());
        return Result.ok(BeanUtil.copyProperties(user, UserVO.class));
    }
    /**
     * 用户评论
     * @param userCommentDTO
     * @return
     */
    @PostMapping("/comment")
    @Operation(summary = "用户评论")
    public Result comment(@Parameter(description = "评论对象",name = "userCommentDTO",required = true) @RequestBody UserCommentDTO userCommentDTO){
        return userService.comment(userCommentDTO);
    }

    /**
     * 用户点赞
     * @param id
     * @return
     */
    @PostMapping("/like/{id}")
    @Operation(summary = "用户点赞")
    public Result like(@Parameter(description = "景点id",name = "id",required = true) @PathVariable Long id){
        return userService.like(id);
    }

    /**
     * 用户收藏
     * @param id
     * @return
     */
    @PostMapping("/collect/{id}")
    @Operation(summary = "用户收藏")
    public Result collect(@Parameter(description = "景点id",name = "id",required = true) @PathVariable Long id){
        return userService.collect(id);
    }

    /**
     * 用户是否点赞
     * @param id
     * @return
     */
    @PostMapping("/isLike")
    @Operation(summary = "用户是否点赞")
    public Result isLike(@Parameter(description = "景点id",name = "id",required = true) @RequestParam Long id){
        return userService.isLike(id);
    }

    /**
     * 用户是否收藏
     * @param id
     * @return
     */
    @PostMapping("/isCollect")
    @Operation(summary = "用户是否收藏")
    public Result isCollect(@Parameter(description = "景点id",name = "id",required = true) @RequestParam Long id){
        return userService.isCollect(id);
    }

    /**
     * 获取用户位置
     * @param request
     * @return
     */
    @GetMapping("/location")
    @Operation(summary = "获取用户位置")
    public Result<LocationVO> location(HttpServletRequest request){
        String ip = ClientUtils.getClientIp(request);
        return userService.location(ip);
    }

    /**
     * 用户历史点赞信息
     * @return
     */
    @GetMapping("/like")
    @Operation(summary = "用户历史点赞信息")
    public Result<List<ScenicVO>> getLikes(){
        return userService.getLikes();
    }

    /**
     * 用户历史评论信息
     * @return
     */
    @GetMapping("/comment")
    @Operation(summary = "用户历史评论信息")
    public Result<List<ScenicVO>> getComments(){
        return userService.getComments();
    }

    /**
     * 用户历史收藏信息
     * @return
     */
    @GetMapping("/collection")
    @Operation(summary = "用户历史收藏信息")
    public Result<List<ScenicVO>> getCollections(){
        return userService.getCollections();
    }

    /**
     * 申请农户资质
     * @param farmerAccessDTO
     * @return
     */
    @PostMapping("/apply_farmer")
    @Operation(summary = "申请农户资质")
    public Result applyFarmer(@Parameter(description = "申请农户信息",name = "farmerAccessDTO",required = true) @RequestBody FarmerAccessDTO farmerAccessDTO){
        Long uid = UserHolder.getUser().getId();
        long pending = farmerAccessMapper.selectCount(
                new LambdaQueryWrapper<FarmerAccess>().eq(FarmerAccess::getUserId, uid).eq(FarmerAccess::getStatus, 0));
        if (pending > 0) {
            return Result.error("您已有待审核的农户申请，请勿重复提交");
        }
        farmerAccessDTO.setUserId(uid);
        FarmerAccess farmerAccess = BeanUtil.copyProperties(farmerAccessDTO, FarmerAccess.class);
        farmerAccess.setStatus(0);
        log.info("farmerAccess:{}",farmerAccess);
        farmerAccessMapper.insert(farmerAccess);
        webSocketServer.sendToAllClient("farmer_access");
        return Result.ok();
    }

    @GetMapping("/search")
    @Operation(summary = "用户搜索")
    public Result<List<VillageBaseVO>> search(@Parameter(description = "搜索内容",name = "content",required = true) @RequestParam String content){
        return userService.search(content);
    }
}

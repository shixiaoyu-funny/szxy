package com.shixiaoyu.xiangyueproject.controller;

import cn.hutool.core.bean.BeanUtil;
import com.shixiaoyu.xiangyueproject.entity.dto.UserCommentDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.UserSetInfoDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.LikeReceivedVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;
import com.shixiaoyu.xiangyueproject.service.LoginService;
import com.shixiaoyu.xiangyueproject.service.UserService;
import com.shixiaoyu.xiangyueproject.utils.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口
 */
@Slf4j
@RestController
@RequestMapping("/ur")
@Tag(name = "用户接口")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final LoginService loginService;

    @Operation(summary = "个人信息设置")
    @PostMapping("/infoset")
    public Result<Void> infoSet(
            @Parameter(description = "用户信息（username/password/phone/email/avatar，均可选，password 明文服务端加密）", required = true)
            @RequestBody UserSetInfoDTO userSetInfoDTO) {
        loginService.infoSet(userSetInfoDTO);
        return Result.ok();
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息")
    public Result<UserVO> info() {
        User user = userService.getById(SecurityUtil.currentUserId());
        return Result.ok(BeanUtil.copyProperties(user, UserVO.class));
    }

    @PostMapping("/comment")
    @Operation(summary = "用户评论")
    public Result<Void> comment(
            @Parameter(description = "评论信息（scenicId必填，score 1-5）", required = true)
            @Valid @RequestBody UserCommentDTO userCommentDTO) {
        userService.comment(userCommentDTO);
        return Result.ok();
    }

    @PostMapping("/like/{id}")
    @Operation(summary = "用户点赞/取消点赞")
    public Result<String> like(@Parameter(description = "景点id", required = true) @PathVariable Long id) {
        return Result.ok(userService.like(id));
    }

    @PostMapping("/collect/{id}")
    @Operation(summary = "用户收藏/取消收藏")
    public Result<String> collect(@Parameter(description = "景点id", required = true) @PathVariable Long id) {
        return Result.ok(userService.collect(id));
    }

    @PostMapping("/isLike")
    @Operation(summary = "用户是否点赞")
    public Result<Boolean> isLike(@Parameter(description = "景点id", required = true) @RequestParam Long id) {
        return Result.ok(userService.isLike(id));
    }

    @PostMapping("/isCollect")
    @Operation(summary = "用户是否收藏")
    public Result<Boolean> isCollect(@Parameter(description = "景点id", required = true) @RequestParam Long id) {
        return Result.ok(userService.isCollect(id));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索（多字段模糊+分页）")
    public Result<?> search(
            @Parameter(description = "搜索内容") @RequestParam(required = false) String content,
            @Parameter(description = "搜索类型（农村-1，景点-2）") @RequestParam(required = false) Integer type,
            @Parameter(description = "页码") @RequestParam(required = false) Integer pageNo,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize) {
        if (type != null && type == 2) {
            return Result.ok(userService.scenicSearch(content, pageNo, pageSize));
        }
        return Result.ok(userService.villageSearch(content, pageNo, pageSize));
    }

    @GetMapping("/like")
    @Operation(summary = "用户历史点赞景点")
    public Result<List<ScenicVO>> getLikes() {
        return Result.ok(userService.getLikes());
    }

    @GetMapping("/comment")
    @Operation(summary = "用户历史评论景点")
    public Result<List<ScenicVO>> getComments() {
        return Result.ok(userService.getComments());
    }

    @GetMapping("/collection")
    @Operation(summary = "用户历史收藏景点")
    public Result<List<ScenicVO>> getCollections() {
        return Result.ok(userService.getCollections());
    }

    @GetMapping("/msg/likes-received")
    @Operation(summary = "收到的点赞动态（消息页）")
    public Result<List<LikeReceivedVO>> likesReceived() {
        return Result.ok(userService.likesReceived());
    }
}

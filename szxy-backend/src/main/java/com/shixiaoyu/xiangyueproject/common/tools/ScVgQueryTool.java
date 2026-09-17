package com.shixiaoyu.xiangyueproject.common.tools;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.entity.po.UserComment;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserCommentMapper;
import com.shixiaoyu.xiangyueproject.service.ScenicService;
import com.shixiaoyu.xiangyueproject.service.UserService;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.shixiaoyu.xiangyueproject.common.config.AIConfiguration.logToolInvoked;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScVgQueryTool {
    private final UserService userService;
    private final ScenicService scenicService;
    private final VillageService villageService;
    private final ScenicMapper scenicMapper;
    private final UserCommentMapper commentMapper;

    @Tool(name = "get_user_liked_scenics", description = """
            调用时机：当用户需要获取自己以往点赞过的景点信息，或者你的回答需要使用到该工具即可随时调用
            功能/作用：查询用户历史点赞过的景点
            """)
    public String getUserLikes() {
        logToolInvoked("get_user_liked_scenics");
        return JSONUtil.toJsonStr(userService.getLikes());
    }

    @Tool(name = "get_user_collected_scenics", description = """
            调用时机：当用户需要获取自己以往收藏过的景点信息，或者你的回答需要使用到该工具即可随时调用
            功能/作用：查询用户历史收藏过的景点
            """)
    public String getUserCollections() {
        logToolInvoked("get_user_collected_scenics");
        return JSONUtil.toJsonStr(userService.getCollections());
    }

    @Tool(name = "search_villages", description = """
            调用时机：当用户需要获取某些农村信息，或者你的回答需要使用到该工具即可随时调用
            功能/作用：多字段模糊搜索农村
            """)
    public String searchVillages(@ToolParam(description = "模糊搜索关键词") String content,
                              @ToolParam(description = "页数（不填默认为1）", required = false) Integer pageNo,
                              @ToolParam(description = "每页大小（不填默认为10）", required = false) Integer pageSize,
                              @ToolParam(description = "搜索类型（农村-1，景点-2）", required = false) Integer type) {
        logToolInvoked("search_villages");
        log.info("search_villages工具传入的参数：{}，{}，{}，{}",content,pageNo==null?1:pageNo,pageSize==null?10:pageSize,type==null?"无":(type==1?"农村":"景点"));
        if (type != null && type == 2) {
            return JSONUtil.toJsonStr(userService.scenicSearch(content, pageNo, pageSize));
        }
        return JSONUtil.toJsonStr(userService.villageSearch(content, pageNo, pageSize));
    }

    @Tool(name = "get_scenic_top10", description = """
            调用时机：当用户需要获取一些好评如潮的、大众喜欢的景点，或者你的回答需要使用到该工具即可随时调用
            功能/作用：获取所有已注册景点中点赞量最高的前10个景点
            """)
    public String getScenicTop10() {
        logToolInvoked("get_scenic_top10");
        return JSONUtil.toJsonStr(scenicService.getTop10Scenic());
    }

    @Tool(name = "get_farm_top10_by_likes", description = """
            调用时机：当用户需要获取一些好评如潮的、大众喜欢的农村，或者你的回答需要使用到该工具即可随时调用
            功能/作用：获取所有已注册农村中点赞量最高的前10个农村
            """)
    public String getFarmTop10ByLikes() {
        logToolInvoked("get_farm_top10_by_likes");
        return JSONUtil.toJsonStr(villageService.villageLikes());
    }

    @Tool(name = "get_farm_top10_by_collections", description = """
            调用时机：当用户需要获取收藏量高、备受欢迎的农村，或者你的回答需要使用到该工具即可随时调用
            功能/作用：获取所有已注册农村中收藏量最高的前10个农村
            """)
    public String getFarmTop10ByCollections() {
        logToolInvoked("get_farm_top10_by_collections");
        return JSONUtil.toJsonStr(villageService.villageCollections());
    }

    @Tool(name = "get_comments_by_scname", description = """
            调用时机：当用户需要获取对应景点或者农村相关的评论、评价，或者你的回答需要使用到该工具即可随时调用
            功能/作用：获取指定景点名称下点赞量最高的10条评论
            """)
    public String getCommentsByScenicName(
            @ToolParam(description = "景点名称") String scenicName
    ) {
        logToolInvoked("get_comments_by_scname");
        log.info("get_comments_by_scname工具传入的参数：{}",scenicName);
        LambdaQueryWrapper<VillageScenic> wrapper = new LambdaQueryWrapper<VillageScenic>().eq(VillageScenic::getName, scenicName);
        VillageScenic villageScenic = scenicMapper.selectOne(wrapper);
        Long scenicId = villageScenic.getId();
        LambdaQueryWrapper<UserComment> wrapper1 = new LambdaQueryWrapper<UserComment>().eq(UserComment::getScenicId, scenicId).in(UserComment::getScore, 4, 5).last("limit 10");
        LambdaQueryWrapper<UserComment> wrapper2 = new LambdaQueryWrapper<UserComment>().eq(UserComment::getScenicId, scenicId).in(UserComment::getScore, 1, 2).last("limit 10");
        List<UserComment> goodComments = commentMapper.selectList(wrapper1);
        List<UserComment> badComments = commentMapper.selectList(wrapper2);
        return "好评：\n" +
                JSONUtil.toJsonStr(goodComments) +
                "\n" +
                "差评：\n" +
                JSONUtil.toJsonStr(badComments);
    }


}

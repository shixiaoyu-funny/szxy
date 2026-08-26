package com.shixiaoyu.xiangyueproject.common.tools;

import cn.hutool.json.JSONUtil;
import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.service.ScenicService;
import com.shixiaoyu.xiangyueproject.service.UserService;
import com.shixiaoyu.xiangyueproject.service.VillageService;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import static com.shixiaoyu.xiangyueproject.common.config.AIConfiguration.logToolInvoked;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScienceAndVillageTool {
    private final UserService userService;
    private final ScenicService scenicService;
    private final VillageService villageService;

    //--------------------- 查询工具 ---------------------

    @Tool(name = "get_user_liked_scenics", description = """
            调用时机：当用户需要获取自己以往点赞过的景点信息，或者你的回答需要使用到该工具即可调用
            功能/作用：查询用户历史点赞过的景点
            """)
    public String getUserLikes() {
        logToolInvoked("get_user_liked_scenics");
        return JSONUtil.toJsonStr(userService.getLikes());
    }

    @Tool(name = "get_user_collected_scenics", description = """
            调用时机：当用户需要获取自己以往收藏过的景点信息，或者你的回答需要使用到该工具即可调用
            功能/作用：查询用户历史收藏过的景点
            """)
    public String getUserCollections() {
        logToolInvoked("get_user_collected_scenics");
        return JSONUtil.toJsonStr(userService.getCollections());
    }

    @Tool(name = "search_villages", description = """
            调用时机：当用户需要获取某些农村信息，或者你的回答需要使用到该工具即可调用
            功能/作用：多字段模糊搜索农村
            """)
    public String searchFarms(@ToolParam(description = "模糊搜索关键词") String content,
                              @ToolParam(description = "页数（不填默认为1）", required = false) Integer pageNo,
                              @ToolParam(description = "每页大小（不填默认为10）", required = false) Integer pageSize,
                              @ToolParam(description = "搜索类型（农村-1，景点-2）",required = false) Integer type) {
        logToolInvoked("search_villages");
        if (type != null && type == 2) {
            return JSONUtil.toJsonStr(userService.scenicSearch(content, pageNo, pageSize));
        }
        return JSONUtil.toJsonStr(userService.villageSearch(content, pageNo, pageSize));
    }

    @Tool(name = "get_scenic_top10", description = """
            调用时机：当用户需要获取一些好评如潮的、大众喜欢的景点，或者你的回答需要使用到该工具即可调用
            功能/作用：获取所有已注册景点中点赞量最高的前10个景点
            """)
    public String getScenicTop10() {
        logToolInvoked("get_scenic_top10");
        return JSONUtil.toJsonStr(scenicService.getTop10Scenic());
    }

    @Tool(name = "get_farm_top10_by_likes", description = """
            调用时机：当用户需要获取一些好评如潮的、大众喜欢的农村，或者你的回答需要使用到该工具即可调用
            功能/作用：获取所有已注册农村中点赞量最高的前10个农村
            """)
    public String getFarmTop10ByLikes() {
        logToolInvoked("get_farm_top10_by_likes");
        return JSONUtil.toJsonStr(villageService.villageLikes());
    }

    @Tool(name = "get_farm_top10_by_collections", description = """
            调用时机：当用户需要获取收藏量高、备受欢迎的农村，或者你的回答需要使用到该工具即可调用
            功能/作用：获取所有已注册农村中收藏量最高的前10个农村
            """)
    public String getFarmTop10ByCollections() {
        logToolInvoked("get_farm_top10_by_collections");
        return JSONUtil.toJsonStr(villageService.villageCollections());
    }


}

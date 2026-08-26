package com.shixiaoyu.xiangyueproject.common.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommonTool {
    @Tool(
            name = "get_current_time",
            description = "用户的问题涉及到当前时间、日期、星期相关的问题时需要调用，返回服务器当前时间"
    )
    public String queryCurTime(){
        return LocalDateTime.now().toString();
    }
}

package com.shixiaoyu.xiangyueproject.entity.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LCCVO {
    private Long id;
    @JsonAlias({"user_id", "userId"})
    private Long userId;
    @JsonAlias({"target_id", "targetId"})
    private Long targetId;
}

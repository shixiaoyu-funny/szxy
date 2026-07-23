package com.shixiaoyu.xiangyueproject.entity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class PageResultDTO {
    @JsonAlias({"page_no", "pageNo"})
    private Integer pageNo;
    @JsonAlias({"page_size", "pageSize"})
    private Integer pageSize;
}

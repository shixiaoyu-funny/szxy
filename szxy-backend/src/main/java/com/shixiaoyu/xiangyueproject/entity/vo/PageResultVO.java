package com.shixiaoyu.xiangyueproject.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResultVO<T> {
    private Long total;
    private List<T> data;
}

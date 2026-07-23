package com.shixiaoyu.xiangyueproject.entity.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InnerMultiResult {
    private Double score;
    private String name;
}

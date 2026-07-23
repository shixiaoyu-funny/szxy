package com.shixiaoyu.xiangyueproject.entity.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalMultiResult {
    private Integer resultNum;
    private List<InnerMultiResult> result;
    private String logId;
}

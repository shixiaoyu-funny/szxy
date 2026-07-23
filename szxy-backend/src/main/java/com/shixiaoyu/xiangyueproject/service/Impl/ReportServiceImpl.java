package com.shixiaoyu.xiangyueproject.service.Impl;

import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.mapper.ReportMapper;
import com.shixiaoyu.xiangyueproject.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {
    private final ReportMapper reportMapper;
    @Override
    public Result<Integer> farmCnt() {
        Integer cnt=reportMapper.farmCnt();
        return Result.ok(cnt);
    }
}

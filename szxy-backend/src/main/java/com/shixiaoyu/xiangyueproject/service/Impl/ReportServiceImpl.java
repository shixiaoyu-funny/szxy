package com.shixiaoyu.xiangyueproject.service.Impl;

import com.shixiaoyu.xiangyueproject.mapper.ReportMapper;
import com.shixiaoyu.xiangyueproject.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 报表实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportMapper reportMapper;

    @Override
    public Integer farmCnt() {
        return reportMapper.farmCnt();
    }

    @Override
    public Integer newVillageCount() {
        return reportMapper.countNewVillage(LocalDate.now().format(DATE_FORMAT));
    }

    @Override
    public Integer newScenicCount() {
        return reportMapper.countNewScenic(LocalDate.now().format(DATE_FORMAT));
    }
}

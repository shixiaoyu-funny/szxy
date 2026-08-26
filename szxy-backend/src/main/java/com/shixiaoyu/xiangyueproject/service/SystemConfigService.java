package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.SystemConfigDTO;
import com.shixiaoyu.xiangyueproject.entity.po.SystemConfig;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.SystemConfigVO;

/**
 * 系统配置服务
 */
public interface SystemConfigService extends IService<SystemConfig> {

    PageResultVO<SystemConfigVO> list(PageResultDTO pageResultDTO, String keyword);

    SystemConfigVO detail(String configKey);

    void create(SystemConfigDTO dto);

    void update(SystemConfigDTO dto);

    void delete(String configKey);
}

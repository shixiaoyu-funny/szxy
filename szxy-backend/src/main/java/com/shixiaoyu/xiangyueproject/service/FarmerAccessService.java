package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.FarmerAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerAccess;
import com.shixiaoyu.xiangyueproject.entity.vo.FarmerAccessVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.enums.AccessStatusEnum;

/**
 * 农户准入申请：游客提交/改提 + 管理端审批
 */
public interface FarmerAccessService extends IService<FarmerAccess> {

    /** 游客：提交或改提申请 */
    void apply(FarmerAccessDTO dto);

    /** 登录用户：本人最新一条申请 */
    FarmerAccessVO mine();

    /** 管理端：分页列表 */
    PageResultVO<FarmerAccessVO> list(PageResultDTO pageResultDTO, AccessStatusEnum status);

    /** 管理端：通过申请 */
    void approve(Long id);

    /** 管理端：拒绝申请 */
    void reject(Long id);
}

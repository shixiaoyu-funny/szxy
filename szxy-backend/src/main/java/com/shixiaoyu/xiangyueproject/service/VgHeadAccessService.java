package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.po.VgHeadAccess;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VgHeadAccessVO;
import com.shixiaoyu.xiangyueproject.enums.VgHeadStatusEnum;

import java.util.List;

/**
 * 村长准入申请
 */
public interface VgHeadAccessService extends IService<VgHeadAccess> {

    /** 农户：申请成为本村村长 */
    void apply();

    /** 本人最新申请 */
    VgHeadAccessVO mine();

    /** 本人全部申请（消息页） */
    List<VgHeadAccessVO> myList();

    /** 本村村长：待我审的列表（status=0） */
    List<VgHeadAccessVO> pendingForChief();

    /** 详情 */
    VgHeadAccessVO detail(Long id);

    /** 村长通过 */
    void chiefApprove(Long id);

    /** 村长拒绝 */
    void chiefReject(Long id);

    /** 管理端分页列表 */
    PageResultVO<VgHeadAccessVO> adminList(PageResultDTO pageResultDTO, VgHeadStatusEnum status);

    /** 管理员通过（终审） */
    void adminApprove(Long id);

    /** 管理员拒绝 */
    void adminReject(Long id);

    /** 本村待审数量（消息角标） */
    int pendingChiefCount();
}

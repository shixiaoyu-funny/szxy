package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.ScenicDTO;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserCommentVO;

import java.util.List;

/**
 * 景点服务（错误时抛 BusinessException）
 */
public interface ScenicService extends IService<VillageScenic> {

    /** 农户/村长在所属村直接新增景点 */
    void register(ScenicDTO scenicDTO);

    /** top10 按点赞量 */
    List<ScenicVO> getTop10Scenic();

    /** 景点详情（含村名） */
    ScenicVO detail(Long id);

    /** 景点评论列表 */
    List<UserCommentVO> getScComments(Long id);

    /** 管理端：全部景点分页 */
    PageResultVO<ScenicVO> adminList(PageResultDTO pageResultDTO);

    /** 管理端：直接新增景点到指定村 */
    void adminAdd(Long villageId, ScenicDTO scenicDTO);

    /** 管理端：修改景点 */
    void adminUpdate(Long id, ScenicDTO scenicDTO);

    /** 管理端：删除景点 */
    void adminDelete(Long id);
}

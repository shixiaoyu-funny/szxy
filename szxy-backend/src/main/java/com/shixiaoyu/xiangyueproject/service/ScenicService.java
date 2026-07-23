package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.ScenicAccessDTO;
import com.shixiaoyu.xiangyueproject.entity.po.ScenicAccess;
import com.shixiaoyu.xiangyueproject.entity.po.UserComment;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserCommentVO;

import java.util.List;

/**
 * 景点服务接口
 */
public interface ScenicService extends IService<ScenicAccess> {
    // 农户申请私人景点
    Result registerPrivateScenic(ScenicAccessDTO dto);

    // 村长申请公共景点
    Result registerPublicScenic(ScenicAccessDTO dto);


    //查询全部景点注册申请表
    Result getScenicAccessList();

    // 获取排行榜 Top 10
    Result<List<ScenicVO>> getTop10Scenic();

    List<UserCommentVO> getScComments(Long id);

    Result<ScenicVO> detail(Long id);
}
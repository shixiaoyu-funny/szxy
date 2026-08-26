package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.UserCommentDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.vo.LikeReceivedVO;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;

import java.util.List;

/**
 * 用户端服务：评论/点赞/收藏/搜索/历史列表（错误时抛 BusinessException）
 */
public interface UserService extends IService<User> {

    void comment(UserCommentDTO userCommentDTO);

    /** 点赞/取消点赞，返回提示 */
    String like(Long scenicId);

    /** 收藏/取消收藏，返回提示 */
    String collect(Long scenicId);

    boolean isLike(Long scenicId);

    boolean isCollect(Long scenicId);

    /** 用户历史点赞的景点 */
    List<ScenicVO> getLikes();

    /** 用户历史评论的景点 */
    List<ScenicVO> getComments();

    /** 用户历史收藏的景点 */
    List<ScenicVO> getCollections();

    /** 多字段模糊分页搜索农村 */
    PageResultVO<VillageBaseVO> villageSearch(String content, Integer pageNo, Integer pageSize);

    /** 收到的点赞动态（他人点赞我创建的景点） */
    List<LikeReceivedVO> likesReceived();

    PageResultVO<ScenicVO> scenicSearch(String content, Integer pageNo, Integer pageSize);
}

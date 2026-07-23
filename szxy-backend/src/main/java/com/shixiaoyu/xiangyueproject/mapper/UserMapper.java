package com.shixiaoyu.xiangyueproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shixiaoyu.xiangyueproject.entity.dto.UserCommentDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.UserCollect;
import com.shixiaoyu.xiangyueproject.entity.po.UserLike;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {
    void comment(UserCommentDTO userCommentDTO);

    // 1. 检查是否点过赞
    Integer checkLike(@Param("userId") Long userId, @Param("targetId") Long targetId);

    // 2. 插入点赞记录
    void insertLike(UserLike userLike);

    // 3. 删除点赞记录（取消点赞）
    void deleteLike(@Param("userId") Long userId, @Param("targetId") Long targetId);

    // 更新景点的点赞总数，step 为 1 表示加1，为 -1 表示减1
    void updateScenicLikeCount(@Param("scenicId") Long scenicId, @Param("step") int step);

    /**
     * 1. 检查是否已收藏
     */
    Integer checkCollect(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 2. 插入收藏记录
     */
    void insertCollect(UserCollect userCollect);

    /**
     * 3. 删除收藏记录
     */
    void deleteCollect(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 4. 更新景点表的收藏总数
     * step 为 1 表示加1，为 -1 表示减1
     */
    void updateScenicCollectCount(@Param("scenicId") Long scenicId, @Param("step") int step);
}

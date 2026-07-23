package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.UserCommentDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.entity.vo.LocationVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.VillageBaseVO;

import java.util.List;

public interface UserService extends IService<User> {
    Result comment(UserCommentDTO userCommentDTO);

    Result like(Long id);

    Result collect(Long id);

    Result<LocationVO> location(String ip);

    Result<List<ScenicVO>> getLikes();

    Result<List<ScenicVO>> getComments();

    Result<List<ScenicVO>> getCollections();

    Result isLike(Long id);

    Result isCollect(Long id);

    Result<List<VillageBaseVO>> search(String content);
}

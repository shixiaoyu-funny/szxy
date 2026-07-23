package com.shixiaoyu.xiangyueproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shixiaoyu.xiangyueproject.entity.po.ManagerAccess;
import com.shixiaoyu.xiangyueproject.entity.vo.ManagerAccessVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ManagerAccessMapper extends BaseMapper<ManagerAccess> {

    /**
            * 将老村长的身份从 2 (村长) 降级为 1 (农户)
            * 虽然这是操作 farm_user 表，但在流程中由管理端触发
     */
    int demoteOldManager(@Param("oldManagerId") Long oldManagerId, @Param("villageId") Long villageId);

    /**
     * 提权新村长 (将 farm_user 表中对应的 type 改为 2)
     */
    int promoteNewManager(@Param("userId") Long userId, @Param("villageId") Long villageId);

    List<ManagerAccessVO> selectManagerAccessList();
}

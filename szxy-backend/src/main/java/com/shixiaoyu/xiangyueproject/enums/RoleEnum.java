package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 系统角色（user.role）——全系统唯一事实源
 *
 * <pre>
 * 1 游客  VISITOR：注册即得；可浏览/搜索农村景点、top10、详情、评论、点赞、收藏
 * 2 农户  FARMER：管理员建档产生；游客全部能力 + 在所属村直接新增景点、查看我的村/我的景点
 *         （所属村 = farm_user.village_id）
 * 3 村长  CHIEF：管理员通过 set_manager 任命；农户全部能力 + 本村农户管理（增改删）
 *         （village_base.manage_id 指向其 user.id，与 role 在事务内同步）
 * 4 管理员 ADMIN：种子数据预置；拥有全部管理端能力（农村CRUD/农户建档/村长任命/景点CRUD/报表），
 *         AdminInterceptor + SecurityUtils.requireAdmin() 双重校验
 * </pre>
 *
 * 约束力：字段类型为强类型枚举，非法角色值编译不过；DB 侧由 schema.sql 的
 * CHECK(role IN (1,2,3,4)) 兜底。
 */
public enum RoleEnum {
    VISITOR(1, "游客"),
    FARMER(2, "农户"),
    CHIEF(3, "村长"),
    ADMIN(4, "管理员");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    RoleEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}

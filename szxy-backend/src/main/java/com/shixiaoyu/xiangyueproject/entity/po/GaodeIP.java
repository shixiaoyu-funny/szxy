package com.shixiaoyu.xiangyueproject.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GaodeIP {
    /**
     * 返回结果状态值
     * 0：失败
     * 1：成功
     */
    private String status;

    /**
     * 返回状态说明
     * status=0 时返回错误原因
     * status=1 时返回 OK
     */
    private String info;

    /**
     * 状态码
     * 10000 代表正确
     */
    private String infocode;
    /**
     * 省份
     */
    private Object province;
    /**
     * 城市
     */
    private Object city;
    /**
     * 地区编码
     */
    private Object adcode;
    /**
     * 矩形区域
     */
    private Object rectangle;
}

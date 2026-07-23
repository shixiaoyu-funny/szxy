package com.shixiaoyu.xiangyueproject.entity.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T>{
    /**
     * 状态码
     */
    private Integer code;
    /**
     * 接收数据
     */
    private T data;
    /**
     * 状态信息
     */
    private String message;

    public static Result<Void> ok(){
        return new Result<>(200, null, "success");
    }

    public static <T> Result<T> ok(T data){
        return new Result<>(200, data, "success");
    }

    public static <T> Result<T> ok(T data,String message){
        return new Result<>(200, data, message);
    }

    public static<T> Result<T> error(Integer code,String message){
        return new Result<>(code, null, message);
    }
    public static<T> Result<T> error(String message){
        return new Result<>(500, null, message);
    }
}

package com.shixiaoyu.xiangyueproject.exception;

import com.shixiaoyu.xiangyueproject.entity.result.Result;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理（对齐 robot-singleton 模式）
 * 业务层直接 throw BusinessException，此处统一转成 Result 返回给前端
 */
@Slf4j
@Hidden // 目的：让 knife4j 忽略这个类
@RestControllerAdvice(basePackages = "com.shixiaoyu.xiangyueproject.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.error("业务异常：", e);
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        // 提取第一条校验失败信息，返回给前端
        log.error("校验异常：", e);
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.error(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        log.error("参数校验异常：", e);
        String msg = e.getConstraintViolations().stream()
                .findFirst().map(v -> v.getMessage()).orElse("参数校验失败");
        return Result.error(400, msg);
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<Void> handleSQLSyntax(BadSqlGrammarException e) {
        log.error("SQL语法错误，完整堆栈如下：", e);
        return Result.error("执行数据库操作时语法有误，请联系管理员进行修复");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleSystem(Exception e) {
        log.error("未捕获的系统异常：", e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}

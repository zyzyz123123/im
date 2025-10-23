package com.zyzyz.im.common;

import lombok.Data;

/**
 * 统一响应结果类
 */
@Data
public class Result<T> {
    private Integer code;      // 状态码
    private String message;    // 提示信息
    private T data;            // 返回数据
    
    // ========== 成功响应 ==========
    
    /**
     * 成功（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    
    /**
     * 成功（不带数据）
     */
    public static <T> Result<T> success() {
        return success(null);
    }
    
    /**
     * 成功（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
    
    // ========== 失败响应 ==========
    
    /**
     * 失败（默认500）
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
    
    /**
     * 失败（自定义状态码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
    
    // ========== 常用状态码 ==========
    
    /**
     * 参数错误（400）
     */
    public static <T> Result<T> badRequest(String message) {
        return error(400, message);
    }
    
    /**
     * 未授权（401）
     */
    public static <T> Result<T> unauthorized(String message) {
        return error(401, message);
    }
    
    /**
     * 禁止访问（403）
     */
    public static <T> Result<T> forbidden(String message) {
        return error(403, message);
    }
    
    /**
     * 未找到（404）
     */
    public static <T> Result<T> notFound(String message) {
        return error(404, message);
    }
}
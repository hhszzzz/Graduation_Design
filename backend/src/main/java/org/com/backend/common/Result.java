package org.com.backend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回结果类
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 成功返回结果
     *
     * @param data 数据
     * @param <T>  泛型
     * @return 返回结果
     */
    public static <T> Result<T> success(T data) {
        return success(ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功返回结果
     *
     * @param message 消息
     * @param data    数据
     * @param <T>     泛型
     * @return 返回结果
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 失败返回结果
     *
     * @param errorCode 错误码
     * @param <T>       泛型
     * @return 返回结果
     */
    public static <T> Result<T> failed(ResultCode errorCode) {
        return failed(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 失败返回结果
     *
     * @param message 消息
     * @param <T>     泛型
     * @return 返回结果
     */
    public static <T> Result<T> failed(String message) {
        return failed(ResultCode.FAILED.getCode(), message);
    }

    /**
     * 失败返回结果
     *
     * @param code    状态码
     * @param message 消息
     * @param <T>     泛型
     * @return 返回结果
     */
    public static <T> Result<T> failed(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 参数验证失败返回结果
     *
     * @param <T> 泛型
     * @return 返回结果
     */
    public static <T> Result<T> validateFailed() {
        return failed(ResultCode.VALIDATE_FAILED);
    }

    /**
     * 参数验证失败返回结果
     *
     * @param message 消息
     * @param <T>     泛型
     * @return 返回结果
     */
    public static <T> Result<T> validateFailed(String message) {
        return failed(ResultCode.VALIDATE_FAILED.getCode(), message);
    }

    /**
     * 未登录返回结果
     *
     * @param <T> 泛型
     * @return 返回结果
     */
    public static <T> Result<T> unauthorized() {
        return failed(ResultCode.UNAUTHORIZED);
    }

    /**
     * 未授权返回结果
     *
     * @param <T> 泛型
     * @return 返回结果
     */
    public static <T> Result<T> forbidden() {
        return failed(ResultCode.FORBIDDEN);
    }
} 
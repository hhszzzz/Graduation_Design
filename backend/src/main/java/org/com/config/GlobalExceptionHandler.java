package org.com.config;

import org.com.common.Result;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class, BindException.class})
    public Result<String> handleValidException(Exception e) {
        BindingResult bindingResult = null;
        if (e instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException) e).getBindingResult();
        } else if (e instanceof BindException) {
            bindingResult = ((BindException) e).getBindingResult();
        }
        
        String message = "参数验证失败";
        if (bindingResult != null && bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        }
        
        return Result.validateFailed(message);
    }
    
    /**
     * 处理认证异常
     */
    @ExceptionHandler(value = BadCredentialsException.class)
    public Result<String> handleBadCredentialsException(BadCredentialsException e) {
        return Result.failed("用户名或密码错误");
    }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler(value = RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        return Result.failed(e.getMessage());
    }
    
    /**
     * 处理其他异常
     */
    @ExceptionHandler(value = Exception.class)
    public Result<String> handleException(Exception e) {
        return Result.failed("服务器内部错误");
    }
} 
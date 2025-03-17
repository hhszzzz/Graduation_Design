package org.com.backend.controller;

import org.com.backend.common.Result;
import org.com.backend.dto.LoginRequest;
import org.com.backend.dto.LoginResponse;
import org.com.backend.dto.RegisterRequest;
import org.com.backend.entity.User;
import org.com.backend.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Resource
    private UserService userService;
    
    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<User> register(@Validated @RequestBody RegisterRequest registerRequest) {
        User user = userService.register(registerRequest);
        return Result.success("注册成功", user);
    }
    
    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = userService.login(loginRequest);
        return Result.success("登录成功", loginResponse);
    }
    
    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        // 从SecurityContextHolder中获取当前登录用户
        org.springframework.security.core.userdetails.User principal = 
                (org.springframework.security.core.userdetails.User) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        
        User user = userService.getUserByUsername(principal.getUsername());
        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }
} 
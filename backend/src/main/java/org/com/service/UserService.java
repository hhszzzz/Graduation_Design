package org.com.service;

import org.com.dto.LoginRequest;
import org.com.dto.LoginResponse;
import org.com.dto.RegisterRequest;
import org.com.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    User register(RegisterRequest registerRequest);
    
    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录结果
     */
    LoginResponse login(LoginRequest loginRequest);
    
    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);
} 
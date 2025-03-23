package org.com.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.com.dto.LoginRequest;
import org.com.dto.LoginResponse;
import org.com.dto.RegisterRequest;
import org.com.entity.User;
import org.com.mapper.UserMapper;
import org.com.service.UserService;
import org.com.utils.JwtTokenUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private PasswordEncoder passwordEncoder;
    
    @Resource
    private AuthenticationManager authenticationManager;
    
    @Resource
    private JwtTokenUtil jwtTokenUtil;
    
    @Value("${jwt.tokenHead:Bearer}")
    private String tokenHead;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        if (getUserByUsername(registerRequest.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
//        System.out.println(registerRequest);
        // 创建用户对象
        User user = new User();
        BeanUtils.copyProperties(registerRequest, user);
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        
        // 设置默认头像
        user.setAvatar("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png");
        
        // 保存用户
        userMapper.insert(user);
        
        return user;
    }
    
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 进行身份验证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), loginRequest.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 生成JWT令牌
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenUtil.generateToken(userDetails);
        
        // 获取用户信息
        User user = getUserByUsername(userDetails.getUsername());
        
        // 构建登录响应
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .token(token)
                .tokenHead(tokenHead)
                .build();
    }
    
    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }
} 
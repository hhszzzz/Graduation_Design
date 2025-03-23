package org.com.security;

import org.com.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Resource
    private UserDetailsService userDetailsService;
    
    @Resource
    private JwtTokenUtil jwtTokenUtil;
    
    @Value("${jwt.header:Authorization}")
    private String tokenHeader;
    
    @Value("${jwt.tokenHead:Bearer}")
    private String tokenHead;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 从请求头获取Authorization
        String authHeader = request.getHeader(this.tokenHeader);
        
        try {
            // 判断是否有token且是否以Bearer开头
            if (authHeader != null && authHeader.startsWith(this.tokenHead)) {
                // 截取有效的token
                String authToken = authHeader.substring(this.tokenHead.length()).trim();
                
                // 检查token是否为空
                if (authToken.isEmpty()) {
                    logger.warn("发现空token");
                    chain.doFilter(request, response);
                    return;
                }
                
                // 检查token是否包含有效字符
                if (!authToken.matches("^[a-zA-Z0-9\\-_\\.]+$")) {
                    logger.warn("Token包含无效字符: " + authToken);
                    chain.doFilter(request, response);
                    return;
                }
                
                try {
                    // 从token中获取用户名
                    String username = jwtTokenUtil.getUsernameFromToken(authToken);
                    
                    // 判断token是否已失效
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 从数据库中获取用户信息
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                        // 验证token是否有效
                        if (jwtTokenUtil.validateToken(authToken, userDetails)) {
                            // 将用户信息存入SecurityContextHolder
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                } catch (Exception e) {
                    logger.error("处理JWT令牌时发生错误: " + e.getMessage(), e);
                    // 继续过滤链，将请求视为未认证
                }
            }
        } catch (Exception e) {
            logger.error("JWT认证过滤器发生异常: " + e.getMessage(), e);
        }
        
        chain.doFilter(request, response);
    }
} 
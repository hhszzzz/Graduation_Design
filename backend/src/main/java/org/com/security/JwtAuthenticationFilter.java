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

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
        // 判断是否有token且是否以Bearer开头
        if (authHeader != null && authHeader.startsWith(this.tokenHead)) {
            // 截取有效的token
            String authToken = authHeader.substring(this.tokenHead.length()).trim();
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
        }
        chain.doFilter(request, response);
    }
} 
package org.com.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 */
@Component
public class JwtTokenUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    private static final String CLAIM_KEY_USERNAME = "sub";
    private static final String CLAIM_KEY_CREATED = "created";
    
    /**
     * 根据用户信息生成token
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USERNAME, userDetails.getUsername());
        claims.put(CLAIM_KEY_CREATED, new Date());
        return generateToken(claims);
    }
    
    /**
     * 从token中获取登录用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            return getClaimFromToken(token, Claims::getSubject);
        } catch (ExpiredJwtException e) {
            logger.warn("JWT令牌已过期: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            logger.warn("JWT令牌格式错误: {}", e.getMessage());
            return null;
        } catch (SignatureException e) {
            logger.warn("JWT签名验证失败: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            logger.warn("JWT解析异常: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("获取用户名异常: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证token是否有效
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            logger.error("验证token异常: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 判断token是否已经失效
     */
    private boolean isTokenExpired(String token) {
        try {
            Date expiredDate = getExpiredDateFromToken(token);
            return expiredDate.before(new Date());
        } catch (Exception e) {
            logger.error("检查token过期异常: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * 从token中获取过期时间
     */
    private Date getExpiredDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    /**
     * 从token中获取指定的claim
     */
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 从token中获取所有的claim
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("解析JWT令牌失败: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 根据claims生成token
     */
    private String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(generateExpirationDate())
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
    
    /**
     * 生成token的过期时间
     */
    private Date generateExpirationDate() {
        return new Date(System.currentTimeMillis() + expiration * 1000);
    }
} 
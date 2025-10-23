package com.zyzyz.im.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    
    // 密钥（生产环境应该放到配置文件）
    private static final String SECRET_KEY = "im-secret-key-for-jwt-token-generation-minimum-256-bits";
    
    // 过期时间：3天
    private static final long EXPIRATION_TIME = 3 * 24 * 60 * 60 * 1000L;
    
    private final SecretKey key;
    
    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
    
    /**
     * 生成Token
     * @param userId 用户ID
     * @return JWT Token
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
        
        return Jwts.builder()
                .setSubject(userId)                          // 设置主题（用户ID）
                .setIssuedAt(now)                            // 签发时间
                .setExpiration(expiryDate)                   // 过期时间
                .signWith(key, SignatureAlgorithm.HS256)    // 签名算法
                .compact();
    }
    
    /**
     * 从Token中获取用户ID
     * @param token JWT Token
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 验证Token是否有效
     * @param token JWT Token
     * @return true=有效，false=无效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Token过期
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            // Token无效
            return false;
        }
    }
    
    /**
     * 获取Token剩余有效时间（秒）
     * @param token JWT Token
     * @return 剩余秒数，-1表示已过期或无效
     */
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Date expiration = claims.getExpiration();
            Date now = new Date();
            
            long remaining = (expiration.getTime() - now.getTime()) / 1000;
            return remaining > 0 ? remaining : -1;
        } catch (JwtException | IllegalArgumentException e) {
            return -1;
        }
    }
}
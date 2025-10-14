package com.SE2025BackEnd_16.project.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    
    // JWT密钥
    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secret;
    
    // JWT过期时间（24小时）
    @Value("${jwt.expiration:86400000}")
    private Long expiration;
    
    // 从token中获取用户名
    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("username", String.class);
    }
    
    // 从token中获取过期时间
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    // 从token中获取指定信息
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    // 从token中获取所有信息
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    // 检查token是否过期
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    // 生成token
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }
    
    // 生成token（带额外信息）
    public String generateToken(String username, Map<String, Object> extraClaims) {
        return createToken(extraClaims, username);
    }
    
    // 创建token
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    // 验证token - 修正版本
    public Boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return (!isTokenExpired(token));
        } catch (SignatureException e) {
            // 签名验证失败
            System.out.println("Invalid JWT signature: " + e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            // token格式错误
            System.out.println("Invalid JWT token: " + e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            // token已过期
            System.out.println("JWT token is expired: " + e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            // 不支持的token
            System.out.println("JWT token is unsupported: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            // 非法参数
            System.out.println("JWT claims string is empty: " + e.getMessage());
            return false;
        } catch (Exception e) {
            // 其他异常
            System.out.println("JWT validation error: " + e.getMessage());
            return false;
        }
    }
    
    // 获取签名密钥
    private SecretKey getSignKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    // 从token中获取用户ID
    public Integer getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Integer.class);
    }
    
    // 从Subject中获取用户ID（当Subject是userId时）
    public Integer getUserIdFromSubject(String token) {
        String subject = getClaimFromToken(token, Claims::getSubject);
        return Integer.valueOf(subject);
    }
    
    // 从token中获取用户角色
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }
    
    // 生成包含用户信息的token（使用userId作为subject）
    public String generateTokenWithUserId(Integer userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("username", username);
        return createToken(claims, userId.toString());
    }
    
    // 生成包含用户信息的token（使用username作为subject）
    public String generateTokenWithUserInfo(String username, Integer userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("username", username);
        return createToken(claims, username);
    }
}

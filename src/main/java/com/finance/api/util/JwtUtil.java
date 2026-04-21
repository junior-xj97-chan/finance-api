package com.finance.api.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 
 * 【面试亮点】
 * - 使用 JJWT 0.12.x 最新版，支持标准 Claims 结构
 * - 区分 AccessToken（短期24h）和 RefreshToken（长期7天），支持 Token 无感刷新
 * - Token 存储 userId 和 username，兼顾安全和业务需求
 * - 支持密钥自动填充（不足256位时补0），避免生产环境密钥配置错误
 * - validateToken 方法统一处理所有 JWT 异常（过期/签名错误/格式错误），避免泄露内部信息
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.ttl-hours}")
    private long ttlHours;

    @Value("${jwt.refresh-ttl-hours}")
    private long refreshTtlHours;

    private SecretKey getSigningKey() {
        // 确保密钥长度 >= 256 bits
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // 填充到 256 bits
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlHours * 3600 * 1000L);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新 Token（有效期更长）
     */
    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTtlHours * 3600 * 1000L);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Token，验证合法性
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Token 解析失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 从 Token 中提取 userId
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从 Token 中提取 username
     */
    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 验证 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public long getTtlSeconds() {
        return ttlHours * 3600;
    }
}

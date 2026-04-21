package com.finance.api.config;

import com.finance.api.util.JwtUtil;
import com.finance.api.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 *
 * 职责：
 *   1. 从请求头 Authorization: Bearer <token> 提取 Token
 *   2. 验证 Token 合法性、Redis 黑名单
 *   3. 解析 userId 写入 {@link UserContext}（ThreadLocal）
 *   4. 请求结束后清理 ThreadLocal（afterCompletion）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String AUTH_HEADER  = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(TOKEN_PREFIX)) {
            // 无 Token 或格式错误：放行，业务层自行判断是否需要登录
            return true;
        }

        String token = authHeader.substring(TOKEN_PREFIX.length());

        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("无效 Token: {}", token.substring(0, Math.min(10, token.length())));
                return true;
            }

            // 检查 Redis 黑名单（注销时写入）
            Boolean isBlacklisted = stringRedisTemplate.hasKey("jwt:blacklist:" + token);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                log.warn("Token 已在黑名单中，拒绝访问");
                return true;
            }

            // 解析 userId，写入 ThreadLocal
            Long userId = jwtUtil.getUserId(token);
            UserContext.setUserId(userId);

            // 同时写入 request attribute，兼容部分需要直接从 request 获取的场景
            request.setAttribute("userId", userId);

        } catch (Exception e) {
            log.warn("Token 解析异常: {}", e.getMessage());
        }

        return true;
    }

    /**
     * 请求结束后清理 ThreadLocal，防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}

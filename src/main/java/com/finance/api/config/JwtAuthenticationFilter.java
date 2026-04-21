package com.finance.api.config;

import com.finance.api.util.JwtUtil;
import com.finance.api.util.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器（OncePerRequestFilter）
 * 
 * 【职责】
 *   1. 每次请求仅执行一次（OncePerRequestFilter 特性，防止重复过滤）
 *   2. 从请求头 Authorization: Bearer <token> 提取 Token
 *   3. 验证 Token 合法性、Redis 黑名单检查
 *   4. 解析 userId，注入 Spring SecurityContext（供 Security 授权使用）
 *   5. 同时写入 UserContext（ThreadLocal），供 Controller/Service 层直接获取用户 ID
 *
 * 【为什么用 Filter 而不是 Interceptor】
 *   - Spring Security Filter Chain 先于 MVC HandlerInterceptor 执行
 *   - 如果用 Interceptor，Security 会在 Interceptor 解析 Token 之前就拦截请求
 *   - 将 JWT 解析放到 Filter 层，确保 Security 授权判断时已有认证信息
 *
 * 【ThreadLocal 安全清理】
 *   - Filter 中写入 UserContext（ThreadLocal）
 *   - JwtInterceptor.afterCompletion() 调用 UserContext.clear() 清理，防止内存泄漏
 *   - 这是一种"写入 Filter，清理在 Interceptor"的分工模式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String AUTH_HEADER  = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 放行 OPTIONS 预检请求（CORS）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(TOKEN_PREFIX)) {
            // 无 Token：放行，未认证请求由 Controller/Service 层自行判断
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(TOKEN_PREFIX.length());

        try {
            // 校验 Token 合法性
            if (!jwtUtil.validateToken(token)) {
                log.warn("JWT 校验失败，拒绝访问");
                filterChain.doFilter(request, response);
                return;
            }

            // 检查 Redis 黑名单（注销/刷新时写入）
            Boolean isBlacklisted = stringRedisTemplate.hasKey("jwt:blacklist:" + token);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                log.warn("Token 在黑名单中，拒绝访问");
                filterChain.doFilter(request, response);
                return;
            }

            // 解析用户信息
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);

            // ① 写入 UserContext（ThreadLocal），供 Service 层使用
            UserContext.setUserId(userId);

            // ② 注入 Spring SecurityContext，触发 Security 的 .authenticated() 规则
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT 认证成功: userId={}, username={}", userId, username);

        } catch (Exception e) {
            log.warn("JWT 解析异常: {}", e.getMessage());
            // 异常时 SecurityContext 保持空，Security 会拒绝已认证接口
        }

        // 继续过滤链（即使解析失败，也继续让 Security 处理未认证的情况）
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 公共路径跳过此过滤器，减少不必要的 Token 解析开销
        return path.startsWith("/api/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.equals("/favicon.ico")
                || path.equals("/error");
    }
}

package com.finance.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 * 
 * 【面试亮点】
 * - 纯 Token 认证，无状态 Session，适合分布式部署
 * - JWT 解析下沉到 Filter 层（在 Spring Security 过滤器链中执行），
 *   解决了 HandlerInterceptor 无法拦截 Security 认证请求的问题
 * - 使用 addFilterBefore 将 JwtAuthenticationFilter 插入 UsernamePasswordAuthenticationFilter 之前，
 *   确保在 Security 授权判断之前完成 Token 解析和 SecurityContext 注入
 * - shouldNotFilter 优化：公共路径跳过 Filter，减少不必要的 Token 解析开销
 * - Redis 黑名单机制：Token 注销/刷新时写入黑名单，支持强制失效
 *
 * 【过滤器执行顺序】
 *   1. JwtAuthenticationFilter（OncePerRequestFilter）
 *      - 解析 Authorization Header，验证 Token，写入 SecurityContext
 *   2. AuthorizationFilter（Spring Security 内置）
 *      - 检查 .authenticated() 规则，使用上一步注入的 Context
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码编码器，供 Service 层注册/登录时使用
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ========== 关闭默认安全机制 ==========
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // ========== Session 管理 ==========
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ========== JWT 认证过滤器（插入到 UsernamePasswordAuthenticationFilter 之前）==========
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ========== 授权规则 ==========
                .authorizeHttpRequests(auth -> auth
                        // 公共路径：放行（Filter 层面直接跳过，不解析 Token）
                        .requestMatchers(
                                "/api/auth/**",           // 登录 / 注册 / 刷新
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()

                        // /api/** 需要认证（Filter 已注入 SecurityContext，认证成功则通过）
                        .requestMatchers("/api/**").authenticated()

                        // 其他所有请求放行（兜底）
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}

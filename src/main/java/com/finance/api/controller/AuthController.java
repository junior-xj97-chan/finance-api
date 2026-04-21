package com.finance.api.controller;

import com.finance.api.common.Result;
import com.finance.api.dto.LoginDTO;
import com.finance.api.dto.RegisterDTO;
import com.finance.api.service.AuthService;
import com.finance.api.vo.LoginVO;
import com.finance.api.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户注册、登录、Token 刷新")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "输入用户名和密码，返回 JWT Token")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(authService.register(dto));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token")
    public Result<LoginVO> refresh(@RequestHeader("Authorization") String authHeader) {
        String refreshToken = authHeader.replace("Bearer ", "");
        return Result.ok(authService.refreshToken(refreshToken));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.ok(authService.getCurrentUser(userId));
    }
}

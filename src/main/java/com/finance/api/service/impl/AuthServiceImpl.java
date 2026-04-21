package com.finance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.api.common.BizCode;
import com.finance.api.common.exception.BusinessException;
import com.finance.api.dto.LoginDTO;
import com.finance.api.dto.RegisterDTO;
import com.finance.api.entity.User;
import com.finance.api.mapper.UserMapper;
import com.finance.api.service.AuthService;
import com.finance.api.util.JwtUtil;
import com.finance.api.vo.LoginVO;
import com.finance.api.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";

    @Override
    public LoginVO login(LoginDTO dto) {
        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .eq(User::getStatus, 1)
        );

        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }

        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(BizCode.PASSWORD_ERROR);
        }

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 将 RefreshToken 存入 Redis（用于主动下线/刷新）
        stringRedisTemplate.opsForValue().set(
                "jwt:refresh:" + user.getId(),
                refreshToken,
                7 * 24 * 3600L,
                TimeUnit.SECONDS
        );

        log.info("用户登录成功: {}", dto.getUsername());

        // 构造响应
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtUtil.getTtlSeconds());
        vo.setUser(toUserVO(user));
        return vo;
    }

    @Override
    public UserVO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(BizCode.USER_ALREADY_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setStatus(1);

        userMapper.insert(user);
        log.info("新用户注册: {}", dto.getUsername());

        return toUserVO(user);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(BizCode.TOKEN_INVALID);
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);

        // 验证 Redis 中的 refreshToken 是否一致
        String storedToken = stringRedisTemplate.opsForValue().get("jwt:refresh:" + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(BizCode.TOKEN_INVALID);
        }

        // 生成新 Token
        String newToken = jwtUtil.generateToken(userId, username);

        LoginVO vo = new LoginVO();
        vo.setToken(newToken);
        vo.setExpiresIn(jwtUtil.getTtlSeconds());
        vo.setUser(getCurrentUser(userId));
        return vo;
    }

    /**
     * 将用户实体转为 VO
     */
    private UserVO toUserVO(User user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getStatus(),
                user.getCreateTime()
        );
    }
}

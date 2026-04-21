package com.finance.api.service;

import com.finance.api.dto.LoginDTO;
import com.finance.api.dto.RegisterDTO;
import com.finance.api.vo.LoginVO;
import com.finance.api.vo.UserVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户注册
     */
    UserVO register(RegisterDTO dto);

    /**
     * 获取当前登录用户信息
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 刷新 Token
     */
    LoginVO refreshToken(String refreshToken);
}

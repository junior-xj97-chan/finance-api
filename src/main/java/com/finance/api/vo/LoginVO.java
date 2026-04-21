package com.finance.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT Token */
    private String token;

    /** Token 类型 */
    private String tokenType = "Bearer";

    /** 过期时间（秒） */
    private Long expiresIn;

    /** 用户信息 */
    private UserVO user;
}

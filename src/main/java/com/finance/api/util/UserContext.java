package com.finance.api.util;

/**
 * 用户上下文工具类（ThreadLocal）
 *
 * 使用方式：
 *   - JwtInterceptor 解析 Token 后调用 UserContext.setUserId(userId)
 *   - Service / Controller 层调用 UserContext.getUserId() 获取当前登录用户
 *   - 请求结束后拦截器 afterCompletion 调用 UserContext.clear() 防止内存泄漏
 */
public final class UserContext {

    private UserContext() {
    }

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前线程的用户 ID（未登录时返回 null）
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 获取当前用户 ID，若未登录则抛出异常
     */
    public static Long getRequiredUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new com.finance.api.common.exception.BusinessException(
                    com.finance.api.common.BizCode.UNAUTHORIZED
            );
        }
        return userId;
    }

    /**
     * 清除当前线程的用户信息（必须在请求结束时调用）
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}

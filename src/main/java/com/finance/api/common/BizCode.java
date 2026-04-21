package com.finance.api.common;

/**
 * 业务错误码枚举
 */
public enum BizCode {

    // ========== 通用 ==========
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    // ========== 用户相关 ==========
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    TOKEN_EXPIRED(1004, "Token 已过期"),
    TOKEN_INVALID(1005, "Token 无效"),

    // ========== 股票相关 ==========
    STOCK_NOT_FOUND(2001, "股票不存在"),
    STOCK_CODE_INVALID(2002, "股票代码格式错误"),
    QUOTE_DATA_EMPTY(2003, "行情数据为空"),

    // ========== 自选股相关 ==========
    WATCHLIST_FULL(3001, "自选股数量已达上限"),
    WATCHLIST_ALREADY_EXISTS(3002, "该股票已在自选中"),
    WATCHLIST_NOT_FOUND(3003, "自选股记录不存在"),

    // ========== 资金流向相关 ==========
    MONEYFLOW_DATA_EMPTY(4001, "资金流向数据为空"),
    HSGT_DATA_EMPTY(4002, "沪深港通数据为空"),
    MARKET_FLOW_EMPTY(4003, "大盘资金流向数据为空");

    private final int code;
    private final String message;

    BizCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

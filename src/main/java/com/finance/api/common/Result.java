package com.finance.api.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * 注意：构造方法使用显式声明，不使用 Lombok @AllArgsConstructor，
 * 原因：Lombok @AllArgsConstructor 在泛型类 Result<T> 上与 Java 25 类型推断不兼容。
 */
@Data
@NoArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private int code;

    /** 消息 */
    private String message;

    /** 数据 */
    private T data;

    // 显式声明的构造方法（JDK 25 兼容）
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(BizCode bizCode) {
        return new Result<>(bizCode.getCode(), bizCode.getMessage(), null);
    }

    public static <T> Result<T> fail(BizCode bizCode, String message) {
        return new Result<>(bizCode.getCode(), message, null);
    }
}

package com.mealorder.common.result;

import java.io.Serializable;

/**
 * 统一响应体：{ code, message, data }
 * code 分段：00000 成功 / Axxxx 参数 / Bxxxx 认证权限 / Cxxxx 业务 / Dxxxx 系统
 * 见 docs/api-convention.md 与项目计划书 §4.11
 */
public class R<T> implements Serializable {

    private final String code;
    private final String message;
    private final T data;

    private R(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null);
    }

    public static <T> R<T> fail(String code, String message) {
        return new R<>(code, message, null);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}

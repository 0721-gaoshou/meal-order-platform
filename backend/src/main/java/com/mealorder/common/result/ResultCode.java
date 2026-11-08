package com.mealorder.common.result;

/**
 * 业务错误码分段：A 参数 / B 认证权限 / C 业务 / D 系统
 */
public enum ResultCode {

    SUCCESS("00000", "成功"),

    PARAM_ERROR("A0001", "参数错误"),
    PARAM_MISSING("A0002", "参数缺失"),

    UNAUTHORIZED("B0001", "未登录或登录已过期"),
    FORBIDDEN("B0002", "无权限"),
    NOT_FOUND("B0003", "资源不存在"),

    BUSINESS_ERROR("C0001", "业务处理失败"),

    SYSTEM_ERROR("D0001", "系统繁忙，请稍后重试"),
    THIRD_PARTY_ERROR("D0002", "第三方服务异常");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

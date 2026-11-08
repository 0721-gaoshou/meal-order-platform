package com.mealorder.common.exception;

import com.mealorder.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：service 层抛出，由全局异常处理器统一转成 R.fail
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

}

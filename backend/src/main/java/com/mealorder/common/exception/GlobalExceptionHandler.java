package com.mealorder.common.exception;

import com.mealorder.common.result.R;
import com.mealorder.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理：任何异常都不允许裸抛给前端，统一转成 R
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：可预期，warn 级别即可 */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** &#064;RequestBody  上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", detail);
        return R.fail(ResultCode.PARAM_ERROR, detail);
    }

    /** 表单绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String detail = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("表单校验失败: {}", detail);
        return R.fail(ResultCode.PARAM_ERROR, detail);
    }

    /** 请求体格式错误（JSON 不合法等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体不可读: {}", e.getMessage());
        return R.fail(ResultCode.PARAM_ERROR, "请求体格式错误");
    }

    /** 兜底：未预期异常，error 日志，不向前端泄露堆栈 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleUnexpected(Exception e) {
        log.error("系统异常", e);
        return R.fail(ResultCode.SYSTEM_ERROR);
    }
}

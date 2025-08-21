package BreakthroughGame.backEnd.UserModule.UserRestful.common.handler;
import org.slf4j.MDC;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ApiResult;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;
import BreakthroughGame.backEnd.UserModule.UserRestful.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 中文备注：集中把各种异常 -> 统一返回结构；默认 HTTP 200（也可切换对应 4xx/5xx）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String traceId() {
        return MDC.get("traceId"); // 中文备注：从 MDC 获取链路ID
    }

    /** 参数校验 - @Valid 对象入参 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ":" + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(ApiResult.fail(ResultCode.A_BAD_REQUEST, msg, traceId()));
    }

    /** 参数校验 - 表单/路径变量绑定错误 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResult<Void>> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ":" + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(ApiResult.fail(ResultCode.A_BAD_REQUEST, msg, traceId()));
    }

    /** 参数校验 - 单参数约束 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(ApiResult.fail(ResultCode.A_BAD_REQUEST, msg, traceId()));
    }

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResult<Void>> handleBiz(BizException ex) {
        return ResponseEntity.ok(ApiResult.fail(ex.getResultCode(), ex.getMessage(), traceId()));
    }

    /** 资源冲突（如唯一键/外键约束） */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.ok(ApiResult.fail(ResultCode.A_CONFLICT, "资源冲突或数据约束失败", traceId()));
    }

    /** 权限相关 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.ok(ApiResult.fail(ResultCode.A_FORBIDDEN, "无访问权限", traceId()));
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleOther(Exception ex, HttpServletRequest request) {
        // 中文备注：可在此打点/告警，必要时记录 request 关键信息（勿记录敏感数据）
        return ResponseEntity.ok(ApiResult.fail(ResultCode.C_SYSTEM_ERROR, "服务器开小差了，请稍后再试", traceId()));
    }
}

package BreakthroughGame.backEnd.UserModule.UserRestful.common.advice;

import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 全局响应包装器
 * 中文备注：将非 ApiResult 的返回统一包装为 ApiResult。
 */
@RestControllerAdvice
@Component
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Resource
    private HttpServletRequest request;
    @Resource
    private ObjectMapper objectMapper; // 中文备注：用于 String 返回兼容

    private String traceId() { return MDC.get("traceId"); }

    /** 是否需要处理 */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 1) 已经是 ApiResult 的不处理
        if (returnType.getParameterType().isAssignableFrom(ApiResult.class)) {
            return false;
        }
        // 2) Swagger/Actuator 等跳过
        String uri = request.getRequestURI();
        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui")
                || uri.startsWith("/actuator")) {
            return false;
        }
        return true;
    }

    /** 包装处理 */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest serverHttpRequest,
                                  ServerHttpResponse serverHttpResponse) {

        // 3) 文件下载/字节流/HTML 等跳过
        if (!MediaType.APPLICATION_JSON.includes(selectedContentType)
                && !MediaType.APPLICATION_PROBLEM_JSON.includes(selectedContentType)) {
            return body;
        }

        // 4) String 返回的特殊处理（否则会类型不匹配）
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(ApiResult.ok(body, traceId()));
            } catch (Exception e) {
                return body; // 中文备注：兜底，尽量不影响正常返回
            }
        }

        // 5) ResponseEntity 的 body 包装（保持原始 header/status）
        if (body instanceof ResponseEntity<?> re) {
            Object newBody = (re.getBody() instanceof ApiResult)
                    ? re.getBody()
                    : ApiResult.ok(re.getBody(), traceId());
            return ResponseEntity.status(re.getStatusCode()).headers(re.getHeaders()).body(newBody);
        }

        // 6) 常规对象包装
        return ApiResult.ok(body, traceId());
    }
}

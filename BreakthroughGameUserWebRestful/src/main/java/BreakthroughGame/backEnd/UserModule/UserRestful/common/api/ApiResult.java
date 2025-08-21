package BreakthroughGame.backEnd.UserModule.UserRestful.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 统一响应体
 * 约定：
 * - 所有成功/失败都返回 200 HTTP，但通过 code 判断业务状态；
 * - 如果你更偏向严格 REST，也可以在全局异常处理中改为对应的 4xx/5xx；此类仍可复用。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // 中文备注：为节省流量，null 字段不序列化
public class ApiResult<T> implements Serializable {
    private boolean success;      // 中文备注：是否业务成功
    private String code;          // 中文备注：业务码（如 "0" 表示成功，Axx/Bxx/Cxx 表示不同模块错误）
    private String message;       // 中文备注：人类可读提示
    private T data;               // 中文备注：业务数据
    private String traceId;       // 中文备注：链路追踪ID，便于排查问题
    private long timestamp;       // 中文备注：服务器时间戳（毫秒）

    public ApiResult() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public static <T> ApiResult<T> ok(T data, String traceId) {
        ApiResult<T> r = new ApiResult<>();
        r.success = true;
        r.code = ResultCode.SUCCESS.getCode();
        r.message = "OK";
        r.data = data;
        r.traceId = traceId;
        return r;
    }

    public static <T> ApiResult<T> okMsg(String msg, String traceId) {
        ApiResult<T> r = new ApiResult<>();
        r.success = true;
        r.code = ResultCode.SUCCESS.getCode();
        r.message = msg;
        r.traceId = traceId;
        return r;
    }

    public static <T> ApiResult<T> fail(ResultCode rc, String msg, String traceId) {
        ApiResult<T> r = new ApiResult<>();
        r.success = false;
        r.code = rc.getCode();
        r.message = msg != null ? msg : rc.getDefaultMessage();
        r.traceId = traceId;
        return r;
    }

}

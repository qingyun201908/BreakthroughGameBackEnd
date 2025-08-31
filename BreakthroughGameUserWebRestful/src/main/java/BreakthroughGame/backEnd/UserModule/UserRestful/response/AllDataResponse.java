// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/response/AllDataResponse.java
package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import org.slf4j.MDC;

/**
 * 中文备注：通用“带数据”的响应基类（泛型）
 * - 保留 AllResponse 为最小父类；本类在其之上增加 data / code / traceId / timestamp
 * - 之后的专用响应（如 LevelSnapshotResponse、DungeonImportResponse）都继承它，但依然是“非泛型类”
 */
public class AllDataResponse<T> extends AllResponse {

    private String code = "0";                     // 中文备注：业务码，默认 "0" 表示 OK
    private String traceId = MDC.get("rid");       // 中文备注：链路追踪，建议在日志 MDC 放 rid
    private long timestamp = System.currentTimeMillis(); // 中文备注：时间戳（毫秒）
    private T data;                                 // 中文备注：真正的数据载荷

    public AllDataResponse() {
        super();
    }

    public AllDataResponse(boolean success, String message, T data) {
        super(success, message);
        this.data = data;
        // 中文备注：traceId / timestamp 在字段定义时已给默认值，如需每次刷新可在此再设置
        this.traceId = MDC.get("rid");
        this.timestamp = System.currentTimeMillis();
    }

    // =============== 便捷工厂方法（通用） ===============
    public static <T> AllDataResponse<T> ok(T data) {
        return new AllDataResponse<>(true, "OK", data);
    }

    public static <T> AllDataResponse<T> ok(String message, T data) {
        return new AllDataResponse<>(true, message, data);
    }

    public static <T> AllDataResponse<T> fail(String message, T data) {
        AllDataResponse<T> r = new AllDataResponse<>(false, message, data);
        r.setCode("ERR");
        return r;
    }

    // =============== getter / setter（中文备注：保留给子类调用） ===============
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

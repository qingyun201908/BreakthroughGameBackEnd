package BreakthroughGame.backEnd.UserModule.UserRestful.common.api;


/**
 * 业务码约定（示例）：
 * 0            成功
 * Axxxx        客户端参数/权限类错误
 * Bxxxx        业务处理异常（可预期）
 * Cxxxx        系统/第三方异常（不可预期）
 */
public enum ResultCode {
    SUCCESS("0", "成功"),
    A_BAD_REQUEST("A4000", "请求参数错误"),
    A_UNAUTHORIZED("A4010", "未登录或凭证失效"),
    A_FORBIDDEN("A4030", "无权限"),
    A_CONFLICT("A4090", "资源冲突"),
    B_BIZ_ERROR("B0001", "业务处理失败"),
    C_SYSTEM_ERROR("C0001", "系统繁忙，请稍后再试"),
    VALIDATE_FAILED("D0001","非法槽位参数"),
    NOT_FOUND("D0002","装备不存在");

    private final String code;
    private final String defaultMessage;

    ResultCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}

package BreakthroughGame.backEnd.UserModule.UserRestful.common.exception;


import BreakthroughGame.backEnd.UserModule.UserRestful.common.api.ResultCode;

/**
 * 自定义业务异常
 * 中文备注：用于在 Service/Domain 层抛出可预期的业务错误，交由全局异常处理器统一转出。
 */
public class BizException extends RuntimeException {
    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getDefaultMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}

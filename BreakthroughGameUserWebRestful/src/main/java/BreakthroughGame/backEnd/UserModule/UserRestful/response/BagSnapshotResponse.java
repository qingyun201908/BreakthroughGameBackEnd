// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/response/BagSnapshotResponse.java
package BreakthroughGame.backEnd.UserModule.UserRestful.response;

// 中文备注：此方案沿用 AllResponse 的 (boolean success, String message) 构造器
//           因此移除对 ResultCode 的依赖
import lombok.Getter;

/** 中文备注：统一响应体扩展（继承 AllResponse，增加 data 字段） */
@Getter
public class BagSnapshotResponse extends AllResponse {
    private final Object data; // 中文备注：携带返回数据（背包快照）

    /** 中文备注：与 AllResponse 构造匹配的构造方法 */
    private BagSnapshotResponse(boolean success, String message, Object data) {
        super(success, message);
        this.data = data;
    }

    /** 中文备注：快捷成功工厂方法 */
    public static BagSnapshotResponse ok(Object data) {
        return new BagSnapshotResponse(true, "OK", data);
    }

    /** 中文备注：快捷失败工厂方法 */
    public static BagSnapshotResponse fail(String msg) {
        return new BagSnapshotResponse(false, msg, null);
    }
}

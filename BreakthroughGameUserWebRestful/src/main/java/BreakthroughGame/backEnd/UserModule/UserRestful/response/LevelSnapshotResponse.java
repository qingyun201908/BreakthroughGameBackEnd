// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/response/LevelSnapshotResponse.java
package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.LevelSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 成功返回时的包裹式响应：在 AllResponse 基础上增加 data 字段承载业务数据
 */
public class LevelSnapshotResponse extends AllResponse {

    @Schema(description = "等级快照数据")
    private LevelSnapshot data;   // 中文备注：实际业务数据载荷

    public LevelSnapshotResponse() {}

    public LevelSnapshotResponse(boolean success, String message, LevelSnapshot data) {
        super(success, message);
        this.data = data;
    }

    public LevelSnapshot getData() { return data; }
    public void setData(LevelSnapshot data) { this.data = data; }

    // —— 便捷静态方法 —— //
    public static LevelSnapshotResponse ok(String message, LevelSnapshot data) {
        return new LevelSnapshotResponse(true, message, data); // 中文备注：成功快速构造
    }
    public static LevelSnapshotResponse ok(LevelSnapshot data) {
        return ok("操作成功", data);
    }
}

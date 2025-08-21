// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/response/DungeonImportResponse.java
package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonImportResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 成功返回时的包裹式响应：在 AllResponse 基础上增加 data 字段承载业务数据
 * 风格与 LevelSnapshotResponse 完全一致（使用 AllResponse 的 (success, message) 构造）
 */
public class DungeonImportResponse extends AllResponse {

    @Schema(description = "导入报告（成功/失败/错误明细）")
    private DungeonImportResult data;   // 中文备注：实际业务数据载荷

    public DungeonImportResponse() {}

    public DungeonImportResponse(boolean success, String message, DungeonImportResult data) {
        super(success, message); // 中文备注：沿用 AllResponse 的成功/消息语义
        this.data = data;
    }

    public DungeonImportResult getData() { return data; }
    public void setData(DungeonImportResult data) { this.data = data; }

    // —— 便捷静态方法 —— //
    public static DungeonImportResponse ok(String message, DungeonImportResult data) {
        return new DungeonImportResponse(true, message, data); // 中文备注：成功快速构造
    }
    public static DungeonImportResponse ok(DungeonImportResult data) {
        return ok("操作成功", data);
    }
}

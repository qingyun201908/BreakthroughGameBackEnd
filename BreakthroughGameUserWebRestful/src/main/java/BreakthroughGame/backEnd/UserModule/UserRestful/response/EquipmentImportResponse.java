// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/response/EquipmentImportResponse.java
package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonImportResult;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.EquipmentImportResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 中文备注：专用响应体（不依赖外部 AllResponse，也可在项目里改成继承你们的 AllResponse）
 */

public class EquipmentImportResponse  extends  AllResponse {
    @Schema(description = "导入报告（成功/失败/错误明细）")
    private EquipmentImportResult data;      // 导入报告


    public EquipmentImportResponse(boolean success, String message, EquipmentImportResult data) {
        super(success, message); // 中文备注：沿用 AllResponse 的成功/消息语义
        this.data = data;
    }

    public EquipmentImportResult getData() { return data; }
    public void setData(EquipmentImportResult data) { this.data = data; }

    // —— 便捷静态方法 —— //
    public static EquipmentImportResponse ok(String message, EquipmentImportResult data) {
        return new EquipmentImportResponse(true, message, data); // 中文备注：成功快速构造
    }
    public static EquipmentImportResponse ok(EquipmentImportResult data) {
        return ok("操作成功", data);
    }
    }


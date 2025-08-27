// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/response/DungeonPassResponse.java
package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonPassResult;
import io.swagger.v3.oas.annotations.media.Schema;

/** 中文备注：通关上报专用响应 */
public class DungeonPassResponse extends AllResponse {

    @Schema(description = "通关上报结果")
    private DungeonPassResult data;

    public DungeonPassResponse() {}

    public DungeonPassResponse(boolean success, String message, DungeonPassResult data) {
        super(success, message);
        this.data = data;
    }

    public DungeonPassResult getData() { return data; }
    public void setData(DungeonPassResult data) { this.data = data; }

    public static DungeonPassResponse ok(String message, DungeonPassResult data) {
        return new DungeonPassResponse(true, message, data);
    }
    public static DungeonPassResponse ok(DungeonPassResult data) {
        return ok("操作成功", data);
    }
}

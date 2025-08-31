// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/response/DungeonPassResponse.java
package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonPassResult;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonPassView;
import io.swagger.v3.oas.annotations.media.Schema;

/** 中文备注：通关上报专用响应（data=通关+掉落） */
public class DungeonPassResponse extends AllResponse {

    @Schema(description = "通关上报结果（含掉落详情）")
    private DungeonPassView data;

    public DungeonPassResponse() {}

    /** 中文备注：新构造器：直接传入组合视图 */
    public DungeonPassResponse(boolean success, String message, DungeonPassView data) {
        super(success, message);
        this.data = data;
    }

    public DungeonPassView getData() { return data; }
    public void setData(DungeonPassView data) { this.data = data; }

    /** ================= 向下兼容的 OK 重载 ================= */

    // 新：传入组合视图
    public static DungeonPassResponse ok(String message, DungeonPassView data) {
        return new DungeonPassResponse(true, message, data);
    }
    public static DungeonPassResponse ok(DungeonPassView data) {
        return ok("操作成功", data);
    }

    // 旧：仅有通关结果时，自动包一层（loot=null）
    public static DungeonPassResponse ok(String message, DungeonPassResult pass) {
        return ok(message, DungeonPassView.builder().pass(pass).loot(null).build());
    }
    public static DungeonPassResponse ok(DungeonPassResult pass) {
        return ok("操作成功", pass);
    }
}

// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/response/DungeonDetailResponse.java
package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonVO;
import io.swagger.v3.oas.annotations.media.Schema;

/** 中文备注：单条详情的包裹式响应 */
public class DungeonDetailResponse extends AllResponse {

    @Schema(description = "副本详情")
    private DungeonVO data;

    public DungeonDetailResponse() {}

    public DungeonDetailResponse(boolean success, String message, DungeonVO data) {
        super(success, message);
        this.data = data;
    }

    public DungeonVO getData() { return data; }
    public void setData(DungeonVO data) { this.data = data; }

    public static DungeonDetailResponse ok(String message, DungeonVO data) {
        return new DungeonDetailResponse(true, message, data);
    }
    public static DungeonDetailResponse ok(DungeonVO data) {
        return ok("操作成功", data);
    }
}

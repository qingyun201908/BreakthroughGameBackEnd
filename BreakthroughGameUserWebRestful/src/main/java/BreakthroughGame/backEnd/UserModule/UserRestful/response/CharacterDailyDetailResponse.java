package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDailyVO;
import io.swagger.v3.oas.annotations.media.Schema;

/** 中文备注：单条详情的响应包装（与 DungeonDetailResponse 风格一致） */
public class CharacterDailyDetailResponse extends AllResponse {

    @Schema(description = "详情数据")
    private CharacterDailyVO data;

    public CharacterDailyDetailResponse() {}

    public CharacterDailyDetailResponse(boolean success, String message, CharacterDailyVO data) {
        super(success, message);
        this.data = data;
    }

    public CharacterDailyVO getData() { return data; }
    public void setData(CharacterDailyVO data) { this.data = data; }

    public static CharacterDailyDetailResponse ok(String message, CharacterDailyVO data) {
        return new CharacterDailyDetailResponse(true, message, data);
    }
    public static CharacterDailyDetailResponse ok(CharacterDailyVO data) {
        return ok("操作成功", data);
    }
}

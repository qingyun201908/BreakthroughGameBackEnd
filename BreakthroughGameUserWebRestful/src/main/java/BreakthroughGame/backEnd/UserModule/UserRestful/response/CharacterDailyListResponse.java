package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.CharacterDailyVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

/** 中文备注：列表查询的响应包装（与 DungeonListResponse 风格一致） */
public class CharacterDailyListResponse extends AllResponse {

    @Schema(description = "分页结果数据")
    private PageResult<CharacterDailyVO> data;

    public CharacterDailyListResponse() {}

    public CharacterDailyListResponse(boolean success, String message, PageResult<CharacterDailyVO> data) {
        super(success, message);
        this.data = data;
    }

    public PageResult<CharacterDailyVO> getData() { return data; }
    public void setData(PageResult<CharacterDailyVO> data) { this.data = data; }

    public static CharacterDailyListResponse ok(String message, PageResult<CharacterDailyVO> data) {
        return new CharacterDailyListResponse(true, message, data);
    }
    public static CharacterDailyListResponse ok(PageResult<CharacterDailyVO> data) {
        return ok("操作成功", data);
    }
}

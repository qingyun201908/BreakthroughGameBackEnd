package BreakthroughGame.backEnd.UserModule.UserRestful.response;

import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonVO;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

/** 中文备注：列表查询的包裹式响应 */
public class DungeonListResponse extends AllResponse {

    @Schema(description = "分页结果数据")
    private PageResult<DungeonVO> data;

    public DungeonListResponse() {}

    public DungeonListResponse(boolean success, String message, PageResult<DungeonVO> data) {
        super(success, message);
        this.data = data;
    }

    public PageResult<DungeonVO> getData() { return data; }
    public void setData(PageResult<DungeonVO> data) { this.data = data; }

    public static DungeonListResponse ok(String message, PageResult<DungeonVO> data) {
        return new DungeonListResponse(true, message, data);
    }
    public static DungeonListResponse ok(PageResult<DungeonVO> data) {
        return ok("操作成功", data);
    }
}

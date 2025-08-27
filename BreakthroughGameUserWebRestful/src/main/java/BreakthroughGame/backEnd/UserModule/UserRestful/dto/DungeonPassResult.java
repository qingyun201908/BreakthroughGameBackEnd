// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/DungeonPassResult.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

/**
 * 中文备注：通关上报后的返回结果载荷
 */
@Data
public class DungeonPassResult {

    @Schema(description = "运行日志ID（dungeon_run_log 主键）")
    private UUID runLogId;

    @Schema(description = "幂等追踪ID")
    private String traceId;

    @Schema(description = "结果码：OK / NO_TIMES / DUPLICATE / INVALID_DUNGEON 等")
    private String resultCode;

    @Schema(description = "是否计入当日次数（即是否真正消耗了次数）")
    private boolean counted;

    @Schema(description = "更新后的当日计数快照")
    private CharacterDailyVO daily;
}

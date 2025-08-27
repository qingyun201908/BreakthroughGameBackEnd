// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/CharacterDailyVO.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 中文备注：对外暴露用的“角色-副本当日计数”视图
 * 字段与前端含义保持一致：runsUsed / runsMax / selected / canChallenge
 */
@Data
public class CharacterDailyVO {
    @Schema(description = "主键ID")
    private UUID id;

    @Schema(description = "角色ID")
    private UUID characterId;

    @Schema(description = "副本唯一键")
    private String dungeonKey;

    @Schema(description = "日期键（JST）")
    private LocalDate dayKey;

    @Schema(description = "今日已用次数（前端 runsUsed）")
    private int runsUsed;

    @Schema(description = "当日次数上限快照（前端 runsMax）")
    private int runsMax;

    @Schema(description = "最近选择的难度（前端 selected）")
    private int selected;

    @Schema(description = "是否可挑战（前端 canChallenge）")
    private boolean canChallenge;

    @Schema(description = "是否允许越过次数限制（forceEnabled 的后端化）")
    private boolean allowOverride;

    @Schema(description = "备注")
    private String notes;

    @Schema(description = "更新时间")
    private OffsetDateTime updatedAt;
}

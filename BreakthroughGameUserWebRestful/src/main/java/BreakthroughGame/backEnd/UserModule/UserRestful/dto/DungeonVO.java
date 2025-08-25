// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/DungeonVO.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 中文备注：对外展示用的简化视图对象（避免直接暴露实体） */
@Data
public class DungeonVO {
    @Schema(description = "主键ID")
    private UUID id;
    @Schema(description = "副本唯一键")
    private String dungeonKey;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "每日上限")
    private int dailyRunsMax;
    @Schema(description = "最大难度")
    private int maxDifficulty;
    @Schema(description = "是否启用")
    private boolean active;
    @Schema(description = "排序值")
    private int sortOrder;
    @Schema(description = "更新时间")
    private OffsetDateTime updatedAt;
}

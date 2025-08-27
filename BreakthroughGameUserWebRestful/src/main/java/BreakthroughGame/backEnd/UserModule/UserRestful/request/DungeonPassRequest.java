// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/request/DungeonPassRequest.java
package BreakthroughGame.backEnd.UserModule.UserRestful.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 中文备注：角色通关上报请求
 */
@Data
public class DungeonPassRequest {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID characterId;

    @Schema(description = "副本唯一键", example = "equip/diamond/coin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String dungeonKey;

    @Schema(description = "难度（对应前端 selected）", example = "3")
    @Min(1)
    private int difficulty = 1;

    @Schema(description = "幂等用追踪ID（相同 traceId 的重复上报不二次计数）", example = "btl_20250825_abc123")
    private String traceId;

    @Schema(description = "业务日键（JST，可不传，默认服务端以 Asia/Tokyo 取 today）", example = "2025-08-25")
    private LocalDate dayKey;

    // —— 可选上传的战斗统计（占位） —— //
    @Schema(description = "用时（秒）", example = "57")
    private Integer timeCostSec;

    // —— 可选上传的战斗统计（占位） —— //
    @Schema(description = "得分/评级", example = "SSS")
    private String score;
}

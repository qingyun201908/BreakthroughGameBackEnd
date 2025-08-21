package BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 挑战流水日志（可选但强烈建议）
 * 记录每次点击“挑战”的尝试，便于风控审计、问题回溯与数据分析
 */
@Entity
@Table(
        name = "dungeon_run_log",
        indexes = {
                @Index(name = "idx_dgrunlog_character_id", columnList = "character_id"),
                @Index(name = "idx_dgrunlog_dungeon_key", columnList = "dungeon_key"),
                @Index(name = "idx_dgrunlog_created_at", columnList = "created_at")
        }
)
@Data
public class DungeonRunLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;                      // 仅存 UUID；不建外键

    @Column(name = "dungeon_key", nullable = false, length = 64)
    private String dungeonKey;                     // 副本键

    @Column(name = "difficulty", nullable = false)
    private int difficulty;                        // 本次挑战选择的难度（= 前端 selected）

    @Column(name = "before_runs_used", nullable = false)
    private int beforeRunsUsed;                    // 挑战前当日次数（便于回溯）

    @Column(name = "after_runs_used", nullable = false)
    private int afterRunsUsed;                     // 挑战后当日次数（未消耗则等于前）

    @Column(name = "result_success", nullable = false)
    private boolean resultSuccess = false;         // 结果是否通关（服务端结算后写入）

    @Column(name = "result_code", length = 64)
    private String resultCode;                     // 结果码（如 OK / NO_TIMES / IN_MAINTENANCE / VALIDATION_FAIL 等）

    @Column(name = "trace_id", length = 64)
    private String traceId;                        // 关联一次战斗/结算的追踪 ID（便于跨服务排查）

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(); // 记录时间
}

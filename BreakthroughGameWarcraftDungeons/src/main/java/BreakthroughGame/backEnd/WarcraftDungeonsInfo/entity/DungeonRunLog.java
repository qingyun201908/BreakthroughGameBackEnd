package BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "dungeon_run_log",
        indexes = {
                @Index(name = "idx_dgrunlog_character_id", columnList = "character_id"),
                @Index(name = "idx_dgrunlog_dungeon_key", columnList = "dungeon_key"),
                @Index(name = "idx_dgrunlog_created_at", columnList = "created_at"),
                // 可选：也可以只保留下面的复合索引（见第3步）
                // @Index(name = "idx_dgrunlog_day_key", columnList = "day_key")
        }
)
@Data
public class DungeonRunLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Column(name = "dungeon_key", nullable = false, length = 64)
    private String dungeonKey;

    @Column(name = "difficulty", nullable = false)
    private int difficulty;

    @Column(name = "before_runs_used", nullable = false)
    private int beforeRunsUsed;

    @Column(name = "after_runs_used", nullable = false)
    private int afterRunsUsed;

    @Column(name = "result_success", nullable = false)
    private boolean resultSuccess = false;

    @Column(name = "result_code", length = 64)
    private String resultCode;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ✅ 新增：业务日（由 DB 依据 created_at 在 Asia/Singapore 时区推导）
    // 作为“当日挑战”的幂等 Key 组成部分
    @Column(name = "day_key", insertable = false, updatable = false)
    private LocalDate dayKey;
}

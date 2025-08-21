package BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 角色-副本：当日挑战计数
 * 前端字段：今日次数 runsUsed / runsMax、难度选择 selected、可挑战 canChallenge
 * 其中 runsMax 取自定义表的快照，便于回溯
 */
@Entity
@Table(
        name = "character_dungeon_daily",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chardgdaily_char_dungeon_day", columnNames = {"character_id", "dungeon_key", "day_key"})
        },
        indexes = {
                @Index(name = "idx_chardgdaily_character_id", columnList = "character_id"),
                @Index(name = "idx_chardgdaily_dungeon_key", columnList = "dungeon_key"),
                @Index(name = "idx_chardgdaily_day_key", columnList = "day_key"),
                @Index(name = "idx_chardgdaily_updated_at", columnList = "updated_at")
        }
)
@Data
public class CharacterDungeonDaily {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;                      // 仅存 UUID；不建外键

    @Column(name = "dungeon_key", nullable = false, length = 64)
    private String dungeonKey;                     // 副本键（如 equip/diamond/coin）

    @Column(name = "day_key", nullable = false)
    private LocalDate dayKey;                      // 当天（JST）键；由服务层按 Asia/Tokyo 计算写入

    @Column(name = "runs_used", nullable = false)
    private int runsUsed = 0;                      // 今日已用次数（= 前端 runsUsed）

    @Column(name = "runs_max_snapshot", nullable = false)
    private int runsMaxSnapshot = 3;               // 当日次数上限快照（= 前端 runsMax）

    @Column(name = "last_selected_difficulty", nullable = false)
    private int lastSelectedDifficulty = 1;        // 最近选择的难度（= 前端 selected，用于下次默认值）

    @Column(name = "allow_override", nullable = false)
    private boolean allowOverride = false;         // 是否允许越过次数限制（前端 forceEnabled 的后端化）

    @Column(name = "notes", length = 255)
    private String notes;                          // 备注（活动白名单来源、GM 操作说明等）

    @Version
    private long version;                          // 乐观锁

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(); // 更新时间

    // —— 衍生属性：可否挑战（与前端 canChallenge 一致）—— //
    @Transient
    public boolean isCanChallenge() {
        // 中文备注：允许越限或当日次数未用满即可挑战
        return allowOverride || (runsUsed < runsMaxSnapshot);
    }
}

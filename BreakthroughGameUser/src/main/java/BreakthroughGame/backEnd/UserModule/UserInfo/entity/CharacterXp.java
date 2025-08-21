package BreakthroughGame.backEnd.UserModule.UserInfo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 人物经验（与 GameCharacter 弱关联：character_id，仅 UUID）
 * 冗余列用于高频查询与排序，避免每次在线计算
 */
@Entity
@Table(
        name = "character_xp",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_characterxp_character_id", columnNames = "character_id")
        },
        indexes = {
                @Index(name = "idx_characterxp_character_id", columnList = "character_id"),
                @Index(name = "idx_characterxp_current_level", columnList = "current_level"),
                @Index(name = "idx_characterxp_total_xp", columnList = "total_xp"),           // 排行榜常用
                @Index(name = "idx_characterxp_updated_at", columnList = "updated_at"),       // 活跃度/最近更新
                @Index(name = "idx_characterxp_progress", columnList = "progress")            // 进度过滤（可选）
        }
)
@Data
public class CharacterXp {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;             // 仅存 UUID；不建外键

    @Column(name = "total_xp", nullable = false)
    private long totalXp = 0L;            // 总经验（非负）

    @Column(name = "current_level", nullable = false)
    private int currentLevel = 1;         // 当前等级（冗余）

    // —— 冗余的阈值/进度类字段 —— //
    @Column(name = "level_start_total_xp", nullable = false)
    private long levelStartTotalXp = 0L;  // 当前等级起点的累计阈值经验

    @Column(name = "next_level_total_xp", nullable = false)
    private long nextLevelTotalXp = 0L;   // 下一等级的累计阈值经验（满级可与起点相同或置 0）

    @Column(name = "xp_into_level", nullable = false)
    private long xpIntoLevel = 0L;        // 本级已获经验（= total_xp - levelStartTotalXp）

    @Column(name = "xp_to_next_level", nullable = false)
    private long xpToNextLevel = 0L;      // 距下一级还需经验（满级为 0）

    @Column(name = "progress", nullable = false)
    private double progress = 0.0;        // 本级进度 0~1（满级恒为 1）

    @Column(name = "is_max_level", nullable = false)
    private boolean isMaxLevel = false;   // 是否满级

    // —— 变动追踪 —— //
    @Column(name = "last_gain_at")
    private OffsetDateTime lastGainAt;    // 最近一次经验变动时间（插入/增减时刷新）

    @Column(name = "last_delta_xp", nullable = false)
    private long lastDeltaXp = 0L;        // 最近一次经验变化量（插入时取 total_xp）

    @Version
    private long version;                 // 乐观锁（防覆盖）

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(); // 更新时间

    public boolean isIsMaxLevel() {
        return false;
    }
}

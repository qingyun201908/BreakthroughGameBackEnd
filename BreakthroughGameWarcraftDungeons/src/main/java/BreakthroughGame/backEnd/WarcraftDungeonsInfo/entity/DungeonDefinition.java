package BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 副本定义（字典表）
 * 对应前端的：标题、最大难度、每日次数上限等可配置项
 * 不与角色绑定；运营变更仅改这里
 */
@Entity
@Table(
        name = "dungeon_definition",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dungeondf_key", columnNames = "dungeon_key")
        },
        indexes = {
                @Index(name = "idx_dungeondf_active", columnList = "is_active"),
                @Index(name = "idx_dungeondf_sort", columnList = "sort_order"),
                @Index(name = "idx_dungeondf_updated_at", columnList = "updated_at")
        }
)
@Data
public class DungeonDefinition {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "dungeon_key", nullable = false, length = 64)
    private String dungeonKey;               // 副本唯一键（如：equip / diamond / coin）

    @Column(name = "title", nullable = false, length = 128)
    private String title;                    // 副本标题（与前端展示一致）

    @Column(name = "daily_runs_max", nullable = false)
    private int dailyRunsMax = 3;            // 每日最大次数（前端 runsMax）

    @Column(name = "max_difficulty", nullable = false)
    private int maxDifficulty = 9;           // 难度上限（前端 maxDifficulty）

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;         // 是否可用（下线/维护时置 false）

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;               // 排序用，越小越靠前

    @Version
    private long version;                    // 乐观锁

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(); // 更新时间
}

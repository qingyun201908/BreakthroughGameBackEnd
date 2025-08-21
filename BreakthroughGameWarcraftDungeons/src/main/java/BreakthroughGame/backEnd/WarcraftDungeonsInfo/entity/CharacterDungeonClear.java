package BreakthroughGame.backEnd.WarcraftDungeonsInfo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 角色-副本：终身通关进度
 * 前端字段：已通关难度 cleared（通常为终身最高通关难度）
 */
@Entity
@Table(
        name = "character_dungeon_clear",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chardgclear_char_dungeon", columnNames = {"character_id", "dungeon_key"})
        },
        indexes = {
                @Index(name = "idx_chardgclear_character_id", columnList = "character_id"),
                @Index(name = "idx_chardgclear_dungeon_key", columnList = "dungeon_key"),
                @Index(name = "idx_chardgclear_highest", columnList = "highest_cleared_difficulty"),
                @Index(name = "idx_chardgclear_updated_at", columnList = "updated_at")
        }
)
@Data
public class CharacterDungeonClear {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;                      // 仅存 UUID；不建外键

    @Column(name = "dungeon_key", nullable = false, length = 64)
    private String dungeonKey;                     // 副本键

    @Column(name = "highest_cleared_difficulty", nullable = false)
    private int highestClearedDifficulty = 0;      // 最高已通关难度（= 前端 cleared）

    @Column(name = "first_cleared_at")
    private OffsetDateTime firstClearedAt;         // 首次达到该难度的时间（可选）

    @Column(name = "last_cleared_at")
    private OffsetDateTime lastClearedAt;          // 最近一次通关该难度的时间（可选）

    @Version
    private long version;                          // 乐观锁

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(); // 更新时间
}

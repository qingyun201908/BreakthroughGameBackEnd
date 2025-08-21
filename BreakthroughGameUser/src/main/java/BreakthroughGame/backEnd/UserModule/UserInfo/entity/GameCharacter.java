package BreakthroughGame.backEnd.UserModule.UserInfo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "characters",
        indexes = {
                @Index(name = "idx_characters_user_id", columnList = "user_id") // 用于按用户查人物的高频查询
        },
        uniqueConstraints = {
                // 若希望同一用户下的人物名唯一，可加此约束（可选）
                @UniqueConstraint(name = "uk_characters_user_name", columnNames = {"user_id", "name"})
        }
)
/**
 * 人物（与用户弱关联：仅保存 userId，不建外键）
 */
@Data
public class GameCharacter {

    @Id
    @GeneratedValue
    private UUID id;                              // 人物ID（主键）

    @Column(name = "user_id", nullable = false)   // 与用户的弱关联字段（无外键约束）
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String name;                          // 人物名（可按需要增加唯一性）

    @Column(name = "exp", nullable = false)
    private long exp;                             // 经验值（可很大，用long）

    @Embedded
    private CombatAttributes attributes;          // 战斗属性（嵌入值对象）

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(); // 创建时间


}

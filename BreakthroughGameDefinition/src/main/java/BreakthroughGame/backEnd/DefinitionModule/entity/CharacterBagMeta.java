package BreakthroughGame.backEnd.DefinitionModule.entity;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/entity/CharacterBagMeta.java


import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 中文备注：人物背包元信息（目前仅容量），每个角色 1 行 */
@Entity
@Table(
        name = "character_bag_meta",
        uniqueConstraints = @UniqueConstraint(name = "uk_bagmeta_character_id", columnNames = "character_id"),
        indexes = {
                @Index(name = "idx_bagmeta_character_id", columnList = "character_id"),
                @Index(name = "idx_bagmeta_updated_at", columnList = "updated_at")
        }
)
@Data
public class CharacterBagMeta {
    @Id @GeneratedValue
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;                // 中文备注：仅存 UUID，不建外键

    @Column(name = "capacity", nullable = false)
    private int capacity = 24;               // 中文备注：默认容量 24，与前端保持

    @Version
    private long version;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}

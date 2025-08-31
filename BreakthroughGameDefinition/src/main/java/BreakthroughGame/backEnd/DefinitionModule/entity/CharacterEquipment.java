package BreakthroughGame.backEnd.DefinitionModule.entity;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/entity/CharacterEquipment.java


import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentSlot; // 中文备注：用你已有的槽位枚举
import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "character_equipment",
        uniqueConstraints = @UniqueConstraint(name = "uk_chequip_character_slot", columnNames = {"character_id","slot"}),
        indexes = {
                @Index(name = "idx_chequip_character_id", columnList = "character_id"),
                @Index(name = "idx_chequip_item_key", columnList = "item_key"),
                @Index(name = "idx_chequip_updated_at", columnList = "updated_at")
        }
)
public class CharacterEquipment {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;                // 中文备注：弱关联，仅存 UUID

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", length = 24, nullable = false)
    private EquipmentSlot slot;              // 中文备注：存枚举名（CLOAK/WRIST/...）

    @Column(name = "item_key", length = 64, nullable = false)
    private String itemKey;                  // 中文备注：图鉴 key（弱关联）

    @Column(name = "name", length = 64, nullable = false)
    private String name;                     // 冗余：装备名

    @Column(name = "rarity", nullable = false)
    private int rarity = 1;                  // 冗余：稀有度

    @Column(name = "icon", length = 256)
    private String icon;                     // 冗余：图标

    @Column(name = "description", columnDefinition = "text")
    private String description;              // 冗余：描述

    @Column(name = "bag_item_id")
    private UUID bagItemId;                  // 冗余：来源背包条目 ID（弱关联）

    @Version
    private long version;                    // 乐观锁

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist(){
        if (id == null) id = UUID.randomUUID();
        // 中文备注：updated_at 交由 DB 默认值，但也做一次兜底
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }
}

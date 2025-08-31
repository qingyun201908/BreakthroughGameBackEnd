package BreakthroughGame.backEnd.DefinitionModule.entity;// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/entity/CharacterEquipmentHistory.java


import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentSlot;
import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "character_equipment_history",
        indexes = {
                @Index(name = "idx_chequip_his_character_id", columnList = "character_id"),
                @Index(name = "idx_chequip_his_created_at", columnList = "created_at")
        }
)
public class CharacterEquipmentHistory {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "character_id", nullable = false)
    private UUID characterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", length = 24, nullable = false)
    private EquipmentSlot slot;

    @Column(name = "old_item_key", length = 64)
    private String oldItemKey;

    @Column(name = "new_item_key", length = 64)
    private String newItemKey;

    @Column(name = "reason", length = 64, nullable = false)
    private String reason;                   // 中文备注：EQUIP / UNEQUIP / SWAP

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist(){
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}

// 文件：BreakthroughGame/backEnd/DefinitionModule/entity/EquipmentDefinitionDungeon.java
package BreakthroughGame.backEnd.DefinitionModule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * 中文备注：图鉴-来源副本映射（无外键） -> equipment_definition__dungeon
 * 复合主键 (equip_key, dungeon_key)；弱关联到 dungeon_definition.dungeon_key
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@IdClass(EquipmentDefinitionDungeon.PK.class)
@Table(name = "equipment_definition__dungeon",
        indexes = @Index(name = "idx_eqdef_dungeon_key", columnList = "dungeon_key"))
public class EquipmentDefinitionDungeon {

    @Id
    @Column(name = "equip_key", length = 64, nullable = false)
    private String equipKey;

    @Id
    @Column(name = "dungeon_key", length = 64, nullable = false)
    private String dungeonKey;

    @Data
    public static class PK implements Serializable {
        private String equipKey;
        private String dungeonKey;
    }
}

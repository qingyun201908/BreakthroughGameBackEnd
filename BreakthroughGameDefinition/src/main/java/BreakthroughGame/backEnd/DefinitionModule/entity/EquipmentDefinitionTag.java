// 文件：BreakthroughGame/backEnd/DefinitionModule/entity/EquipmentDefinitionTag.java
package BreakthroughGame.backEnd.DefinitionModule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * 中文备注：图鉴标签（无外键） -> equipment_definition_tag
 * 复合主键 (equip_key, tag)；仅弱关联到主表的 equip_key
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@IdClass(EquipmentDefinitionTag.PK.class)
@Table(name = "equipment_definition_tag",
        indexes = @Index(name = "idx_eqtag_tag", columnList = "tag"))
public class EquipmentDefinitionTag {

    @Id
    @Column(name = "equip_key", length = 64, nullable = false)
    private String equipKey;

    @Id
    @Column(name = "tag", length = 48, nullable = false)
    private String tag;

    @Data
    public static class PK implements Serializable {
        private String equipKey;
        private String tag;
    }
}

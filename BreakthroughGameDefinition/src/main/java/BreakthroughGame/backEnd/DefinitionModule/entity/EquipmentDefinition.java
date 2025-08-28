// 文件：BreakthroughGame/backEnd/DefinitionModule/entity/EquipmentDefinition.java
package BreakthroughGame.backEnd.DefinitionModule.entity;

import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentRarity;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentSlot;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CombatAttributes; // 中文备注：复用 attr_* 列
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 中文备注：装备图鉴主表实体（无外键、不与人物/实例绑定）
 * - 不再使用 @ElementCollection/@ManyToMany，标签与来源改为独立表弱关联
 * - 注意：@PrePersist/@PreUpdate 内仅处理本表字段
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "equipment_definition",
        indexes = {
                @Index(name = "idx_eqdef_key", columnList = "equip_key", unique = true),
                @Index(name = "idx_eqdef_slot", columnList = "slot"),
                @Index(name = "idx_eqdef_rarity", columnList = "rarity"),
                @Index(name = "idx_eqdef_enabled", columnList = "enabled")
        })
public class EquipmentDefinition {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;                                            // 主键

    @Column(name = "equip_key", length = 64, nullable = false, unique = true)
    private String equipKey;                                    // 业务唯一键（如 glove_taiji）

    @Column(name = "name", length = 64, nullable = false)
    private String name;                                        // 展示名

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", length = 24, nullable = false)
    private EquipmentSlot slot;                                 // 槽位

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", length = 24, nullable = false)
    private EquipmentRarity rarity;                             // 稀有度

    @Column(name = "star_max", nullable = false)
    private int starMax;                                        // 星级上限

    // ⚠ 若你的数据库列名是 level_req，请把下面一行的 name 改为 "level_req"
    @Column(name = "level_requirement", nullable = false)
    private int levelRequirement;                               // 佩戴等级（图鉴展示用）

    @Column(name = "color_hex", length = 16)
    private String colorHex;                                    // 品质色（可空：DB 触发器按稀有度补）

    @Column(name = "icon", length = 128)
    private String icon;                                        // 图标

    @Column(name = "description", length = 512)
    private String description;                                 // 描述

    @Embedded
    private CombatAttributes attributes;                        // 白字属性（列名为 attr_*）

    @Column(name = "enabled", nullable = false)
    private boolean enabled;                                    // 是否可见

    @Column(name = "release_version", length = 32)
    private String releaseVersion;                              // 投放版本

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;                                      // 排序（越小越靠前）

    @Column(name = "version", nullable = false)
    private long version;                                       // 乐观锁（应用维护）

    // 时间戳由 DB 触发器维护，这里只读映射（可选）
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        // 中文备注：若未显式给 colorHex，可让 DB 触发器按 rarity 自动补色（保持空）
        if (releaseVersion == null) releaseVersion = "1.0.0";
        // 其余时间戳交由 DB 触发器
    }

    @PreUpdate
    public void preUpdate() {
        // 中文备注：更新时间交由 DB 触发器；这里不处理
    }



}

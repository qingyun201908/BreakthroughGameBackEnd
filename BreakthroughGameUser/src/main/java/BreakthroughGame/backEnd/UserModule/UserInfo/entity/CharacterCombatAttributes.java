// 文件：BreakthroughGame/backEnd/UserModule/UserInfo/entity/CharacterCombatAttributes.java
// 中文备注：角色战斗属性“独立实体 + 独立表”（一名角色一行，弱关联）

package BreakthroughGame.backEnd.UserModule.UserInfo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "character_combat_attributes",
        // 中文备注：保证一名角色仅有一行战斗属性记录；并对更新时间建立查询索引
        uniqueConstraints = @UniqueConstraint(name = "uk_ccattrs_character_id", columnNames = "character_id"),
        indexes = {
                @Index(name = "idx_ccattrs_updated_at", columnList = "updated_at")
        }
)
public class CharacterCombatAttributes {

    // ===== 主键（UUID） =====
    @Id
    @GeneratedValue // 中文备注：由应用/ORM 生成 UUID；若希望用数据库 gen_random_uuid()，可改由服务层填充
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    private UUID id;

    // ===== 关联角色（弱关联，无外键） =====
    @Column(name = "character_id", nullable = false, columnDefinition = "uuid")
    private UUID characterId; // 中文备注：角色 UUID（与 GameCharacter.id 对应，但不加外键）

    // ===== 战斗属性（纯数值或百分比） =====
    @Column(name = "attr_attack", nullable = false)
    private int attack = 0;                   // 攻击（纯数值）

    @Column(name = "attr_atk_speed_pct", nullable = false)
    private int attackSpeedPercent = 100;     // 攻速（百分比；100=100%）

    @Column(name = "attr_crit_rate_pct", nullable = false)
    private int critRatePercent = 0;          // 暴击率（百分比）

    @Column(name = "attr_crit_dmg_pct", nullable = false)
    private int critDamagePercent = 150;      // 暴击伤害（百分比；150=150%）

    @Column(name = "attr_hit_pct", nullable = false)
    private int hitPercent = 100;             // 命中（百分比；100=100%）

    @Column(name = "attr_penetration", nullable = false)
    private int penetration = 0;              // 穿透（纯数值）

    // ===== 五行与混沌 =====
    @Column(name = "attr_metal", nullable = false)
    private int metal = 0;                    // 金

    @Column(name = "attr_wood", nullable = false)
    private int wood = 0;                     // 木

    @Column(name = "attr_water", nullable = false)
    private int water = 0;                    // 水

    @Column(name = "attr_fire", nullable = false)
    private int fire = 0;                     // 火

    @Column(name = "attr_earth", nullable = false)
    private int earth = 0;                    // 土

    @Column(name = "attr_chaos", nullable = false)
    private int chaos = 0;                    // 混沌

    // ===== 乐观锁 & 更新时间 =====
    @Version
    @Column(name = "version", nullable = false)
    private long version = 0;                 // 中文备注：乐观锁（配合并发更新）

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;         // 中文备注：更新时间（应用层维护）

    // ===== 生命周期回调：统一更新时间 =====
    @PrePersist @PreUpdate
    private void touch() {
        this.updatedAt = OffsetDateTime.now(); // 中文备注：统一维护 updated_at
    }

    // ===== 工厂方法：按角色初始化默认面板 =====
    public static CharacterCombatAttributes initFor(UUID characterId) {
        return CharacterCombatAttributes.builder()
                .id(null)
                .characterId(characterId)
                .attack(0)
                .attackSpeedPercent(0)
                .critRatePercent(0)
                .critDamagePercent(0)
                .hitPercent(0)
                .penetration(0)
                .metal(0).wood(0).water(0).fire(0).earth(0).chaos(0)
                .version(0)
                .build();
    }
}

package BreakthroughGame.backEnd.UserModule.UserInfo.entity;

import jakarta.persistence.*;
import lombok.Data;

// 可嵌入的值对象：专门存储战斗属性（会被扁平化成列）
@Data
@Embeddable
public class CombatAttributes {
    // 数值字段（可按需要设置默认值/范围校验）
    @Column(name = "attr_attack", nullable = false)
    private int attack;                 // 攻击

    @Column(name = "attr_atk_speed_pct", nullable = false)
    private int attackSpeedPercent;     // 攻速（百分比，示例图为100%）

    @Column(name = "attr_crit_rate_pct", nullable = false)
    private int critRatePercent;        // 暴击（百分比）

    @Column(name = "attr_crit_dmg_pct", nullable = false)
    private int critDamagePercent;      // 暴伤（百分比）

    @Column(name = "attr_hit_pct", nullable = false)
    private int hitPercent;             // 命中（百分比）

    @Column(name = "attr_penetration", nullable = false)
    private int penetration;            // 穿透（纯数值）

    // 五行与混沌（按需要扩展/调整类型）
    @Column(name = "attr_metal", nullable = false)
    private int metal;                  // 金
    @Column(name = "attr_wood", nullable = false)
    private int wood;                   // 木
    @Column(name = "attr_water", nullable = false)
    private int water;                  // 水
    @Column(name = "attr_fire", nullable = false)
    private int fire;                   // 火
    @Column(name = "attr_earth", nullable = false)
    private int earth;                  // 土
    @Column(name = "attr_chaos", nullable = false)
    private int chaos;                  // 混沌


}

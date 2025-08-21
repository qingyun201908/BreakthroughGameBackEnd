// 文件：CharacterDTO.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;


import BreakthroughGame.backEnd.UserModule.UserInfo.entity.CombatAttributes;
import BreakthroughGame.backEnd.UserModule.UserInfo.entity.GameCharacter;

import java.util.UUID;

/**
 * 中文备注：登录返回的人物快照（对外 DTO，不直接暴露 Entity）
 */
public class CharacterDTO {
    private UUID id;            // 角色ID
    private String name;        // 名称
    private long exp;           // 经验
    private Attributes attributes; // 战斗属性（内嵌对象，字段更清晰）

    // —— 工厂方法：从实体映射到 DTO ——
    public static CharacterDTO from(GameCharacter ch) {
        CharacterDTO dto = new CharacterDTO();
        dto.id = ch.getId();
        dto.name = ch.getName();
        dto.exp = ch.getExp();
        dto.attributes = Attributes.from(ch.getAttributes());
        return dto;
    }

    // —— Getter/Setter（可用 lombok，这里手写便于直接使用） ——
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getExp() { return exp; }
    public void setExp(long exp) { this.exp = exp; }
    public Attributes getAttributes() { return attributes; }
    public void setAttributes(Attributes attributes) { this.attributes = attributes; }

    // ================= 内嵌属性对象 =================
    public static class Attributes {
        private int attack;              // 攻击
        private int attackSpeedPercent;  // 攻速%
        private int critRatePercent;     // 暴击%
        private int critDamagePercent;   // 暴伤%
        private int hitPercent;          // 命中%
        private int penetration;         // 穿透
        private int metal;               // 金
        private int wood;                // 木
        private int water;               // 水
        private int fire;                // 火
        private int earth;               // 土
        private int chaos;               // 混沌

        public static Attributes from(CombatAttributes a) {
            Attributes x = new Attributes();
            if (a != null) {
                x.attack = a.getAttack();
                x.attackSpeedPercent = a.getAttackSpeedPercent();
                x.critRatePercent = a.getCritRatePercent();
                x.critDamagePercent = a.getCritDamagePercent();
                x.hitPercent = a.getHitPercent();
                x.penetration = a.getPenetration();
                x.metal = a.getMetal();
                x.wood = a.getWood();
                x.water = a.getWater();
                x.fire = a.getFire();
                x.earth = a.getEarth();
                x.chaos = a.getChaos();
            }
            return x;
        }

        // —— Getter/Setter ——（中文备注：省略也可用 lombok）
        public int getAttack() { return attack; }
        public void setAttack(int attack) { this.attack = attack; }
        public int getAttackSpeedPercent() { return attackSpeedPercent; }
        public void setAttackSpeedPercent(int attackSpeedPercent) { this.attackSpeedPercent = attackSpeedPercent; }
        public int getCritRatePercent() { return critRatePercent; }
        public void setCritRatePercent(int critRatePercent) { this.critRatePercent = critRatePercent; }
        public int getCritDamagePercent() { return critDamagePercent; }
        public void setCritDamagePercent(int critDamagePercent) { this.critDamagePercent = critDamagePercent; }
        public int getHitPercent() { return hitPercent; }
        public void setHitPercent(int hitPercent) { this.hitPercent = hitPercent; }
        public int getPenetration() { return penetration; }
        public void setPenetration(int penetration) { this.penetration = penetration; }
        public int getMetal() { return metal; }
        public void setMetal(int metal) { this.metal = metal; }
        public int getWood() { return wood; }
        public void setWood(int wood) { this.wood = wood; }
        public int getWater() { return water; }
        public void setWater(int water) { this.water = water; }
        public int getFire() { return fire; }
        public void setFire(int fire) { this.fire = fire; }
        public int getEarth() { return earth; }
        public void setEarth(int earth) { this.earth = earth; }
        public int getChaos() { return chaos; }
        public void setChaos(int chaos) { this.chaos = chaos; }
    }
}

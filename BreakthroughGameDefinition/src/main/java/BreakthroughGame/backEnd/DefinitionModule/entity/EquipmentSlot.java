// 文件：BreakthroughGame/backEnd/DefinitionModule/entity/EquipmentSlot.java
package BreakthroughGame.backEnd.DefinitionModule.entity;

import lombok.Getter;

/** 中文备注：与前端 BagPage.vue 的 slots.key 对齐 */
@Getter
public enum EquipmentSlot {
    CLOAK("cloak","斗篷"),
    WRIST("wrist","手腕"),
    GLOVE("glove","手套"),
    ARMOR("armor","衣服"),
    BELT("belt","腰带"),
    PANTS("pants","裤子"),
    SHOES("shoes","鞋子"),
    RING("ring","戒指"),
    NECKLACE("necklace","项链"),
    SHOULDER("shoulder","护肩");

    private final String frontKey;
    private final String label;
    EquipmentSlot(String k, String l){ this.frontKey = k; this.label = l; }

    /** 中文备注：可通过前端 key 反查枚举 */
    public static EquipmentSlot ofFrontKey(String key){
        for (var s : values()) if (s.frontKey.equalsIgnoreCase(key)) return s;
        throw new IllegalArgumentException("Unknown slot key: "+key);
    }
}

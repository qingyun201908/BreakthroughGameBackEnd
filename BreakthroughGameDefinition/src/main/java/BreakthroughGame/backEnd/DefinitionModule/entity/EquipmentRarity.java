// 文件：BreakthroughGame/backEnd/DefinitionModule/entity/EquipmentRarity.java
package BreakthroughGame.backEnd.DefinitionModule.entity;

import lombok.Getter;

/** 中文备注：稀有度（颜色用于前端小圆点） */
@Getter
public enum EquipmentRarity {
    COMMON("#9ca3af"),
    UNCOMMON("#22c55e"),
    RARE("#3b82f6"),
    EPIC("#a855f7"),
    LEGENDARY("#f59e0b"),
    MYTHIC("#ef4444");

    private final String colorHex;
    EquipmentRarity(String hex){ this.colorHex = hex; }
}

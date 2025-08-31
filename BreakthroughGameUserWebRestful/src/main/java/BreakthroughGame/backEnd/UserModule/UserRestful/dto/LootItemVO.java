// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/LootItemVO.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import lombok.*;

/**
 * 中文备注：掉落展示用 VO，直接返回前端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LootItemVO {
    private String equipKey;      // 中文备注：装备唯一键
    private String name;          // 中文备注：装备名
    private Integer rarity;       // 中文备注：稀有度（1~N 的整型）
    private String rarityName;    // 中文备注：稀有度枚举名（如 RARE/EPIC），便于前端文案
    private String icon;          // 中文备注：图标 URL 或资源键
    private String description;   // 中文备注：描述
}

// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/EquipableItemDto.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import java.util.UUID;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EquipableItemDto {
    private UUID id;             // 中文备注：背包条目 id（用于“穿戴/卸下”时回传）
    private String itemKey;      // 中文备注：物品唯一键（与 equip_key 对齐）
    private String name;         // 中文备注：展示名（优先背包冗余名）
    private Integer star;        // 中文备注：星级（直接使用背包 rarity：1~5）:contentReference[oaicite:5]{index=5}
    private String color;        // 中文备注：颜色（优先 ed.color_hex，否则后备色）:contentReference[oaicite:6]{index=6}
    private String slotKey;      // 中文备注：部位（ed.slot，前端直接使用）:contentReference[oaicite:7]{index=7}
    private String icon;         // 中文备注：图标（ed.icon）:contentReference[oaicite:8]{index=8}
    private String description;  // 中文备注：描述（优先背包 desc，否则 ed.description）:contentReference[oaicite:9]{index=9}
    private Integer qty;         // 中文备注：可穿戴数量（>=1）:contentReference[oaicite:10]{index=10}
}

// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/DungeonPassView.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import lombok.*;
import BreakthroughGame.backEnd.UserModule.UserRestful.dto.DungeonPassResult;

/**
 * 中文备注：通关结果 + 掉落信息的组合视图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DungeonPassView {
    private DungeonPassResult pass;  // 中文备注：原有通关结果
    private LootItemVO loot;         // 中文备注：本次掉落；若副本未配置掉落则为 null
}

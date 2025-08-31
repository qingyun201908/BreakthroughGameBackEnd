// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/BagItemView.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 中文备注：前端背包条目视图 */
@Data
@AllArgsConstructor
public class BagItemView {
    private String id;       // 中文备注：后端 UUID 字符串
    private String name;
    private int qty;
    private String type;     // 中文备注：与前端一致（consumable/equipment/...）
    private int rarity;      // 1~5
    private String desc;
}

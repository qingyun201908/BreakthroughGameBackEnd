// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/BagQuery.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

import lombok.Data;

/** 中文备注：查询参数载体，用于服务层 */
@Data
public class BagQuery {
    private String q;          // 模糊搜索（name）
    private String type;       // 物品类型（all/consumable/...）
    private String sort;       // default/rarity/name/qty
}

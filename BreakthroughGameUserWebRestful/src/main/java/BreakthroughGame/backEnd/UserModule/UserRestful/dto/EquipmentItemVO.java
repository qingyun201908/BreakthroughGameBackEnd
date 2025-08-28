package BreakthroughGame.backEnd.UserModule.UserRestful.dto;// 文件：BreakthroughGame/backEnd/DefinitionModule/rest/dto/EquipmentItemVO.java


import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentRarity;
import BreakthroughGame.backEnd.DefinitionModule.entity.EquipmentSlot;
import lombok.*;

import java.util.List;

/** 中文备注：列表项/详情的通用展示 VO */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EquipmentItemVO {
    private String equipKey;                 // 中文备注：装备唯一键
    private String name;                     // 中文备注：展示名
    private EquipmentSlot slot;              // 中文备注：槽位（含中/英）
    private EquipmentRarity rarity;          // 中文备注：稀有度（含颜色）
    private Integer starMax;                 // 中文备注：星级上限（若实体没有，可去掉）
    private Boolean enabled;                 // 中文备注：是否启用（若实体没有，可去掉）

    // ====== 新增字段 ======
    private String description;              // 中文备注：装备描述（来自实体字段）
    private String icon;                     // 中文备注：图标地址（来自实体字段）


    private List<String> tags;               // 中文备注：标签列表（弱关联）
    private List<String> dungeons;           // 中文备注：来源副本 key 列表（弱关联）
}

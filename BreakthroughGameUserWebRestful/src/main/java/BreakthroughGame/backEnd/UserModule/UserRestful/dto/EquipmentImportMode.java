// 文件：BreakthroughGame/backEnd/UserModule/UserRestful/dto/EquipmentImportMode.java
package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

/** 中文备注：导入策略 */
public enum EquipmentImportMode {
    UPSERT,        // 默认：有则更新、无则插入
    INSERT_ONLY,   // 仅插入：遇到已存在 equip_key 视为冲突
    UPDATE_ONLY    // 仅更新：遇到不存在 equip_key 视为冲突
}

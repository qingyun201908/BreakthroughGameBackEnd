package BreakthroughGame.backEnd.UserModule.UserRestful.dto;// 文件：BreakthroughGame/backEnd/DungeonModule/DailyChallenge/dto/DungeonImportMode.java


/**
 * 中文备注：导入模式
 * UPSERT：存在即更新；INSERT_ONLY：仅新增，若存在则报错
 */
public enum DungeonImportMode {
    UPSERT,
    INSERT_ONLY
}

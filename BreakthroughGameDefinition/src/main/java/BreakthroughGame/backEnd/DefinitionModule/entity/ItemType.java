package BreakthroughGame.backEnd.DefinitionModule.entity;

/** 中文备注：物品类型枚举，与前端保持一致 */
public enum ItemType {
    consumable, // 消耗品
    equipment,  // 装备
    material,   // 材料
    quest,      // 任务
    misc;       // 其他

    /** 中文备注：忽略大小写解析（前端 query-string 友好） */
    public static ItemType fromNullable(String s) {
        if (s == null) return null;
        try { return ItemType.valueOf(s.toLowerCase()); }
        catch (Exception ignore) { return null; }
    }
}

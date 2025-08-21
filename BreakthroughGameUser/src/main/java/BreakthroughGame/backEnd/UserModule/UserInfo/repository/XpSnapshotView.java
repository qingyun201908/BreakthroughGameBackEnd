package BreakthroughGame.backEnd.UserModule.UserInfo.repository;

/** upsert 返回投影接口（列名需与 SQL AS 别名一致） */
public interface XpSnapshotView {
    Long getTotalXp();
    Integer getCurrentLevel();
    Long getLevelStartTotalXp();
    Long getNextLevelTotalXp();
    Long getXpIntoLevel();
    Long getXpToNextLevel();
    Double getProgress();
    Boolean getIsMaxLevel();
}

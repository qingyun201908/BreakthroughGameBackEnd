package BreakthroughGame.backEnd.UserModule.UserRestful.dto;

// 文件：BreakthroughGame/backEnd/CharacterModule/CharacterXp/service/dto/LevelSnapshot.java
/** 等级快照 DTO（对外返回用） */
public class LevelSnapshot {
    public final long totalXp;
    public final int level;
    public final double progress;
    public final long xpIntoLevel;
    public final long xpToNextLevel;
    public final long levelStartTotalXp;
    public final long nextLevelTotalXp;
    public final boolean isMaxLevel;

    public LevelSnapshot(long totalXp, int level, double progress,
                         long xpIntoLevel, long xpToNextLevel,
                         long levelStartTotalXp, long nextLevelTotalXp,
                         boolean isMaxLevel) {
        this.totalXp = totalXp;
        this.level = level;
        this.progress = progress;
        this.xpIntoLevel = xpIntoLevel;
        this.xpToNextLevel = xpToNextLevel;
        this.levelStartTotalXp = levelStartTotalXp;
        this.nextLevelTotalXp = nextLevelTotalXp;
        this.isMaxLevel = isMaxLevel;
    }
}

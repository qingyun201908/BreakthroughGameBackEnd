package BreakthroughGame.backEnd.Utils;// 文件：BreakthroughGame/backEnd/common/utils/LogLevelingUtil.java


/**
 * 对数型等级曲线工具类（无状态，可复用）
 * 与数据库函数口径一致
 */
public final class LogLevelingUtil {

    /** 参数对象（可由配置注入） */
    public static final class Params {
        public final int maxLevel;
        public final double base;
        public final double scale;
        public final double soft;

        public Params(int maxLevel, double base, double scale, double soft) {
            if (maxLevel < 1) throw new IllegalArgumentException("maxLevel must be >= 1");
            if (scale <= 0 || soft <= 0) throw new IllegalArgumentException("scale & soft must be > 0");
            this.maxLevel = maxLevel;
            this.base = base;
            this.scale = scale;
            this.soft = soft;
        }
    }

    private LogLevelingUtil() {}

    /** 连续等级（可视化用） */
    public static double levelContinuous(long totalXp, Params p) {
        double xp = Math.max(0L, totalXp);
        return p.base + p.scale * Math.log1p(xp / p.soft);
    }

    /** 离散等级（floor + clamp） */
    public static int levelForTotalXp(long totalXp, Params p) {
        int lvl = (int) Math.floor(levelContinuous(totalXp, p));
        if (lvl < (int) Math.floor(p.base)) lvl = (int) Math.floor(p.base);
        if (lvl > p.maxLevel) lvl = p.maxLevel;
        return lvl;
    }

    /** 反函数：达到等级 L 的累计总经验 */
    public static long totalXpToReachLevel(int level, Params p) {
        if (level <= Math.floor(p.base)) return 0L;
        double need = p.soft * Math.expm1((level - p.base) / p.scale);
        if (Double.isInfinite(need) || need < 0) return Long.MAX_VALUE;
        return (long) Math.floor(need + 0.5);
    }

    /** 从 L 到 L+1 需要的经验 */
    public static long xpForLevel(int level, Params p) {
        if (level >= p.maxLevel) return 0L;
        long a = totalXpToReachLevel(level, p);
        long b = totalXpToReachLevel(level + 1, p);
        return Math.max(0L, b - a);
    }

    /** 本级进度（0~1，满级恒为1） */
    public static double progressWithinLevel(long totalXp, Params p) {
        int lvl = levelForTotalXp(totalXp, p);
        if (lvl >= p.maxLevel) return 1.0;
        long start = totalXpToReachLevel(lvl, p);
        long need  = xpForLevel(lvl, p);
        if (need <= 0) return 1.0;
        return Math.min(1.0, Math.max(0.0, (double)(totalXp - start) / need));
    }
}

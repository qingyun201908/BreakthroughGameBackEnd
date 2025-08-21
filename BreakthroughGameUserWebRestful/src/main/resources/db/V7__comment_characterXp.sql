-- 文件名建议：V2_0__comment_character_xp.sql
-- 作用：为 character_xp 表及相关对象添加中文备注（持久化到系统目录）
-- 注意：若你的 schema 不是 public，请把 "public." 前缀改成对应 schema

DO $$
BEGIN
  -- ===== 表与列备注 =====
  IF to_regclass('public.character_xp') IS NOT NULL THEN
    COMMENT ON TABLE character_xp IS
      '人物经验冗余快照表：以 UUID 软关联人物（无外键），存储总经验与基于对数等级曲线预计算的等级/进度等派生字段。';

    COMMENT ON COLUMN character_xp.id IS
      '主键：UUID。';
    COMMENT ON COLUMN character_xp.character_id IS
      '人物ID（与 characters.id 弱关联，无外键约束）。保证一人一条记录。';
    COMMENT ON COLUMN character_xp.total_xp IS
      '累计总经验（非负）。写入时由触发器统一刷新冗余字段。';
    COMMENT ON COLUMN character_xp.current_level IS
      '当前离散等级（冗余）：由对数等级曲线计算并封顶，触发器自动维护。';
    COMMENT ON COLUMN character_xp.level_start_total_xp IS
      '当前等级起点累计阈值经验：达到当前等级所需的总经验（冗余）。';
    COMMENT ON COLUMN character_xp.next_level_total_xp IS
      '下一等级累计阈值经验：达到 L+1 所需的总经验（冗余；满级时与起点相同或为 0）。';
    COMMENT ON COLUMN character_xp.xp_into_level IS
      '本级已获经验：= total_xp - level_start_total_xp（冗余，最低为 0）。';
    COMMENT ON COLUMN character_xp.xp_to_next_level IS
      '距下一级所需经验：= max(0, next_level_total_xp - total_xp)，满级为 0（冗余）。';
    COMMENT ON COLUMN character_xp.progress IS
      '本级进度（0~1，满级恒为 1）：由阈值差分计算（冗余）。';
    COMMENT ON COLUMN character_xp.is_max_level IS
      '是否满级标记（冗余）。';
    COMMENT ON COLUMN character_xp.last_gain_at IS
      '最近一次经验变动时间（插入或 total_xp 变更时更新）。';
    COMMENT ON COLUMN character_xp.last_delta_xp IS
      '最近一次经验变化量（插入取 total_xp；更新取差值）。';
    COMMENT ON COLUMN character_xp.version IS
      '乐观锁版本号（JPA @Version）。';
    COMMENT ON COLUMN character_xp.updated_at IS
      '数据更新时间（触发器统一刷新）。';

    -- 唯一约束备注（保证一人一条记录）
    IF EXISTS (
      SELECT 1 FROM pg_constraint
      WHERE conname = 'uk_characterxp_character_id'
        AND conrelid = 'public.character_xp'::regclass
    ) THEN
      COMMENT ON CONSTRAINT uk_characterxp_character_id ON character_xp IS
        '唯一约束：同一 character_id 仅允许一条经验记录（无外键，UUID 软关联）。';
    END IF;
  END IF;

  -- ===== 索引备注 =====
  IF to_regclass('public.idx_characterxp_character_id') IS NOT NULL THEN
    COMMENT ON INDEX idx_characterxp_character_id IS
      '按人物ID快速定位经验记录（写扩散场景下的 upsert/查询加速）。';
  END IF;

  IF to_regclass('public.idx_characterxp_current_level') IS NOT NULL THEN
    COMMENT ON INDEX idx_characterxp_current_level IS
      '按当前等级排序/过滤的排行榜常用索引。';
  END IF;

  IF to_regclass('public.idx_characterxp_total_xp') IS NOT NULL THEN
    COMMENT ON INDEX idx_characterxp_total_xp IS
      '按总经验排序/打分的辅助索引（配合等级作为次序）。';
  END IF;

  IF to_regclass('public.idx_characterxp_updated_at') IS NOT NULL THEN
    COMMENT ON INDEX idx_characterxp_updated_at IS
      '按更新时间筛选活跃角色或做增量抓取的查询加速。';
  END IF;

  IF to_regclass('public.idx_characterxp_progress') IS NOT NULL THEN
    COMMENT ON INDEX idx_characterxp_progress IS
      '按本级进度（0~1）过滤/推荐（如“快要升级的人物”）的查询加速。';
  END IF;
END
$$;

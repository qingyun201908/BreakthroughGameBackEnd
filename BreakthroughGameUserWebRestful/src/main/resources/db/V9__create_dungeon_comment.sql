-- 中文备注：与实体注解保持一致的基础索引
CREATE INDEX IF NOT EXISTS idx_chardgdaily_character_id  ON character_dungeon_daily(character_id);
CREATE INDEX IF NOT EXISTS idx_chardgdaily_dungeon_key   ON character_dungeon_daily(dungeon_key);
CREATE INDEX IF NOT EXISTS idx_chardgdaily_day_key       ON character_dungeon_daily(day_key);
CREATE INDEX IF NOT EXISTS idx_chardgdaily_updated_at    ON character_dungeon_daily(updated_at);

-- 中文备注：针对“某角色+某天 按更新时间倒序”的查询，建议一个复合索引（更高效）
CREATE INDEX IF NOT EXISTS idx_chardgdaily_char_day_updated
  ON character_dungeon_daily(character_id, day_key, updated_at DESC);

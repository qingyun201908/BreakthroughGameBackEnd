-- 经验表（含所有冗余列，一步到位，减少后续迁移）
CREATE TABLE IF NOT EXISTS character_xp (
  id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  character_id           uuid NOT NULL,                        -- 弱关联 characters.id（无外键）
  total_xp               bigint NOT NULL DEFAULT 0,            -- 总经验（非负）
  current_level          int    NOT NULL DEFAULT 1,            -- 当前等级（冗余）
  level_start_total_xp   bigint NOT NULL DEFAULT 0,            -- 当前级起点累计阈值
  next_level_total_xp    bigint NOT NULL DEFAULT 0,            -- 下一级累计阈值（满级与起点一致或0）
  xp_into_level          bigint NOT NULL DEFAULT 0,            -- 本级已获经验
  xp_to_next_level       bigint NOT NULL DEFAULT 0,            -- 距下一级所需经验
  progress               double precision NOT NULL DEFAULT 0,  -- 本级进度（0~1）
  is_max_level           boolean NOT NULL DEFAULT FALSE,       -- 是否满级
  last_gain_at           timestamptz NULL,                     -- 最近一次经验变动时间
  last_delta_xp          bigint NOT NULL DEFAULT 0,            -- 最近一次经验变化量
  version                bigint NOT NULL DEFAULT 0,            -- 乐观锁（JPA @Version）
  updated_at             timestamptz NOT NULL DEFAULT now(),   -- 更新时间
  CONSTRAINT uk_characterxp_character_id UNIQUE (character_id)
);

-- 经验表常用索引
CREATE INDEX IF NOT EXISTS idx_characterxp_character_id ON character_xp(character_id);
CREATE INDEX IF NOT EXISTS idx_characterxp_current_level ON character_xp(current_level);
CREATE INDEX IF NOT EXISTS idx_characterxp_total_xp ON character_xp(total_xp);
CREATE INDEX IF NOT EXISTS idx_characterxp_updated_at ON character_xp(updated_at);
CREATE INDEX IF NOT EXISTS idx_characterxp_progress ON character_xp(progress);

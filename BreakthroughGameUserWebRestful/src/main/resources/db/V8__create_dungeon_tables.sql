-- ================================
-- V8_create_dungeon_tables.sql
-- 每日挑战相关表（无外键）+ 中文备注
-- ================================

-- 可选：用于生成 UUID 主键（若已在其他版本开启可忽略）
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================
-- 1) 副本定义（字典表）
-- =========================================
CREATE TABLE IF NOT EXISTS dungeon_definition (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),               -- 主键
    dungeon_key       varchar(64)  NOT NULL,                                    -- 副本唯一键（如 equip/diamond/coin）
    title             varchar(128) NOT NULL,                                    -- 展示标题
    daily_runs_max    integer      NOT NULL DEFAULT 3,                          -- 每日最大次数（前端 runsMax）
    max_difficulty    integer      NOT NULL DEFAULT 9,                          -- 难度上限（前端 maxDifficulty）
    is_active         boolean      NOT NULL DEFAULT true,                       -- 是否启用
    sort_order        integer      NOT NULL DEFAULT 0,                          -- 排序（越小越靠前）
    version           bigint       NOT NULL DEFAULT 0,                          -- 乐观锁
    updated_at        timestamptz  NOT NULL DEFAULT now(),                      -- 更新时间
    CONSTRAINT uk_dungeondf_key UNIQUE (dungeon_key),                           -- 唯一键约束
    CONSTRAINT ck_dungeondf_runs_max_nonneg CHECK (daily_runs_max >= 0),        -- 检查：非负
    CONSTRAINT ck_dungeondf_max_diff CHECK (max_difficulty >= 1)                -- 检查：难度至少为 1
);

-- 索引（便于过滤/排序/最近更新）
CREATE INDEX IF NOT EXISTS idx_dungeondf_active     ON dungeon_definition (is_active);
CREATE INDEX IF NOT EXISTS idx_dungeondf_sort       ON dungeon_definition (sort_order);
CREATE INDEX IF NOT EXISTS idx_dungeondf_updated_at ON dungeon_definition (updated_at);

-- 备注
COMMENT ON TABLE  dungeon_definition                         IS '副本定义（字典表）：每日次数上限、最大难度、是否启用等配置';
COMMENT ON COLUMN dungeon_definition.id                      IS '主键 UUID（gen_random_uuid）';
COMMENT ON COLUMN dungeon_definition.dungeon_key             IS '副本唯一键（不使用外键）';
COMMENT ON COLUMN dungeon_definition.title                   IS '副本标题（前端展示）';
COMMENT ON COLUMN dungeon_definition.daily_runs_max          IS '每日最大次数（前端 runsMax）';
COMMENT ON COLUMN dungeon_definition.max_difficulty          IS '难度上限（前端 maxDifficulty）';
COMMENT ON COLUMN dungeon_definition.is_active               IS '是否启用';
COMMENT ON COLUMN dungeon_definition.sort_order              IS '排序值（越小越靠前）';
COMMENT ON COLUMN dungeon_definition.version                 IS '乐观锁版本号（应用侧维护）';
COMMENT ON COLUMN dungeon_definition.updated_at              IS '更新时间（默认 now()）';

-- =========================================
-- 2) 角色-副本：终身通关进度（cleared）
-- =========================================
CREATE TABLE IF NOT EXISTS character_dungeon_clear (
    id                           uuid         PRIMARY KEY DEFAULT gen_random_uuid(), -- 主键
    character_id                 uuid         NOT NULL,                              -- 角色 UUID（无外键）
    dungeon_key                  varchar(64)  NOT NULL,                              -- 副本键
    highest_cleared_difficulty   integer      NOT NULL DEFAULT 0,                    -- 最高已通关难度（前端 cleared）
    first_cleared_at             timestamptz,                                         -- 首次达到该难度时间
    last_cleared_at              timestamptz,                                         -- 最近一次达到该难度时间
    version                      bigint       NOT NULL DEFAULT 0,                    -- 乐观锁
    updated_at                   timestamptz  NOT NULL DEFAULT now(),                -- 更新时间
    CONSTRAINT uk_chardgclear_char_dungeon UNIQUE (character_id, dungeon_key),       -- 角色+副本 唯一
    CONSTRAINT ck_chardgclear_nonneg CHECK (highest_cleared_difficulty >= 0)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_chardgclear_character_id  ON character_dungeon_clear (character_id);
CREATE INDEX IF NOT EXISTS idx_chardgclear_dungeon_key   ON character_dungeon_clear (dungeon_key);
CREATE INDEX IF NOT EXISTS idx_chardgclear_highest       ON character_dungeon_clear (highest_cleared_difficulty);
CREATE INDEX IF NOT EXISTS idx_chardgclear_updated_at    ON character_dungeon_clear (updated_at);

-- 备注
COMMENT ON TABLE  character_dungeon_clear                               IS '角色-副本：终身通关进度；记录最高已通关难度';
COMMENT ON COLUMN character_dungeon_clear.id                            IS '主键 UUID';
COMMENT ON COLUMN character_dungeon_clear.character_id                  IS '角色 UUID（不建外键）';
COMMENT ON COLUMN character_dungeon_clear.dungeon_key                   IS '副本键（不建外键）';
COMMENT ON COLUMN character_dungeon_clear.highest_cleared_difficulty    IS '最高已通关难度（= 前端 cleared）';
COMMENT ON COLUMN character_dungeon_clear.first_cleared_at              IS '首次达到该难度的时间';
COMMENT ON COLUMN character_dungeon_clear.last_cleared_at               IS '最近一次达到该难度的时间';
COMMENT ON COLUMN character_dungeon_clear.version                       IS '乐观锁版本号';
COMMENT ON COLUMN character_dungeon_clear.updated_at                    IS '更新时间';

-- =========================================
-- 3) 角色-副本：当日挑战计数（runsUsed / runsMax / selected）
-- =========================================
CREATE TABLE IF NOT EXISTS character_dungeon_daily (
    id                         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),   -- 主键
    character_id               uuid        NOT NULL,                                -- 角色 UUID（无外键）
    dungeon_key                varchar(64) NOT NULL,                                -- 副本键
    day_key                    date        NOT NULL,                                -- 换日后的“当天键”（由服务层按 JST 计算）
    runs_used                  integer     NOT NULL DEFAULT 0,                      -- 当日已用次数（前端 runsUsed）
    runs_max_snapshot          integer     NOT NULL DEFAULT 3,                      -- 当日次数上限快照（源自定义表）
    last_selected_difficulty   integer     NOT NULL DEFAULT 1,                      -- 最近选择难度（前端 selected）
    allow_override             boolean     NOT NULL DEFAULT false,                  -- 是否允许越限（前端 forceEnabled 的后端化）
    notes                      varchar(255),                                         -- 备注：白名单来源/GM说明等
    version                    bigint      NOT NULL DEFAULT 0,                      -- 乐观锁
    updated_at                 timestamptz NOT NULL DEFAULT now(),                  -- 更新时间
    CONSTRAINT uk_chardgdaily_char_dungeon_day UNIQUE (character_id, dungeon_key, day_key),
    CONSTRAINT ck_chardgdaily_runs_used_nonneg  CHECK (runs_used >= 0),
    CONSTRAINT ck_chardgdaily_runs_max_nonneg   CHECK (runs_max_snapshot >= 0),
    CONSTRAINT ck_chardgdaily_last_selected     CHECK (last_selected_difficulty >= 1)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_chardgdaily_character_id ON character_dungeon_daily (character_id);
CREATE INDEX IF NOT EXISTS idx_chardgdaily_dungeon_key  ON character_dungeon_daily (dungeon_key);
CREATE INDEX IF NOT EXISTS idx_chardgdaily_day_key      ON character_dungeon_daily (day_key);
CREATE INDEX IF NOT EXISTS idx_chardgdaily_updated_at   ON character_dungeon_daily (updated_at);

-- 备注
COMMENT ON TABLE  character_dungeon_daily                             IS '角色-副本：当日挑战计数与选择；runsUsed/runsMax/selected';
COMMENT ON COLUMN character_dungeon_daily.id                          IS '主键 UUID';
COMMENT ON COLUMN character_dungeon_daily.character_id                IS '角色 UUID（不建外键）';
COMMENT ON COLUMN character_dungeon_daily.dungeon_key                 IS '副本键（不建外键）';
COMMENT ON COLUMN character_dungeon_daily.day_key                     IS '当日键（JST 计算，服务层写入）';
COMMENT ON COLUMN character_dungeon_daily.runs_used                   IS '今日已用次数（= 前端 runsUsed）';
COMMENT ON COLUMN character_dungeon_daily.runs_max_snapshot           IS '当日次数上限快照（= 前端 runsMax）';
COMMENT ON COLUMN character_dungeon_daily.last_selected_difficulty    IS '最近一次选择的挑战难度（= 前端 selected）';
COMMENT ON COLUMN character_dungeon_daily.allow_override              IS '是否允许越限挑战（活动/白名单/G M）';
COMMENT ON COLUMN character_dungeon_daily.notes                       IS '备注信息';
COMMENT ON COLUMN character_dungeon_daily.version                     IS '乐观锁版本号';
COMMENT ON COLUMN character_dungeon_daily.updated_at                  IS '更新时间';

-- =========================================
-- 4) 挑战流水日志（审计与分析）
-- =========================================
CREATE TABLE IF NOT EXISTS dungeon_run_log (
    id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(), -- 主键
    character_id     uuid         NOT NULL,                              -- 角色 UUID（无外键）
    dungeon_key      varchar(64)  NOT NULL,                              -- 副本键
    difficulty       integer      NOT NULL,                              -- 本次挑战选择的难度
    before_runs_used integer      NOT NULL DEFAULT 0,                    -- 挑战前已用次数
    after_runs_used  integer      NOT NULL DEFAULT 0,                    -- 挑战后已用次数
    result_success   boolean      NOT NULL DEFAULT false,                -- 是否通关（结算后更新）
    result_code      varchar(64),                                        -- 结果码（OK/NO_TIMES/IN_MAINTENANCE 等）
    trace_id         varchar(64),                                        -- 追踪 ID（对接战斗/结算链路）
    created_at       timestamptz  NOT NULL DEFAULT now(),                -- 创建时间
    CONSTRAINT ck_dgrunlog_difficulty_pos CHECK (difficulty >= 1),
    CONSTRAINT ck_dgrunlog_before_nonneg  CHECK (before_runs_used >= 0),
    CONSTRAINT ck_dgrunlog_after_nonneg   CHECK (after_runs_used  >= 0)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_dgrunlog_character_id ON dungeon_run_log (character_id);
CREATE INDEX IF NOT EXISTS idx_dgrunlog_dungeon_key  ON dungeon_run_log (dungeon_key);
CREATE INDEX IF NOT EXISTS idx_dgrunlog_created_at   ON dungeon_run_log (created_at);

-- 备注
COMMENT ON TABLE  dungeon_run_log                     IS '每日挑战流水：每次点击“挑战”的尝试与结果（审计/分析）';
COMMENT ON COLUMN dungeon_run_log.id                  IS '主键 UUID';
COMMENT ON COLUMN dungeon_run_log.character_id        IS '角色 UUID（不建外键）';
COMMENT ON COLUMN dungeon_run_log.dungeon_key         IS '副本键（不建外键）';
COMMENT ON COLUMN dungeon_run_log.difficulty          IS '本次挑战选择的难度';
COMMENT ON COLUMN dungeon_run_log.before_runs_used    IS '挑战前的当日已用次数';
COMMENT ON COLUMN dungeon_run_log.after_runs_used     IS '挑战后的当日已用次数';
COMMENT ON COLUMN dungeon_run_log.result_success      IS '是否通关（结算后更新）';
COMMENT ON COLUMN dungeon_run_log.result_code         IS '结果码：OK/NO_TIMES/IN_MAINTENANCE/VALIDATION_FAIL 等';
COMMENT ON COLUMN dungeon_run_log.trace_id            IS '追踪 ID（串联战斗/结算日志）';
COMMENT ON COLUMN dungeon_run_log.created_at          IS '记录创建时间';

-- gen_random_uuid() 需要该扩展（只需初始化一次）
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- characters 表：仅保存 user_id（弱关联），不加外键约束
CREATE TABLE IF NOT EXISTS characters (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),  -- 主键
    user_id uuid NOT NULL,                          -- 与用户的弱关联字段（无外键）
    name varchar(50) NOT NULL,                      -- 角色名
    exp bigint NOT NULL DEFAULT 0,                  -- 经验值
    created_at timestamptz NOT NULL DEFAULT now(),  -- 创建时间

    -- —— 战斗属性（扁平化为列） ——
    attr_attack int NOT NULL DEFAULT 0,             -- 攻击
    attr_atk_speed_pct int NOT NULL DEFAULT 100,    -- 攻速（百分比）
    attr_crit_rate_pct int NOT NULL DEFAULT 0,      -- 暴击（百分比）
    attr_crit_dmg_pct int NOT NULL DEFAULT 150,     -- 暴伤（百分比）
    attr_hit_pct int NOT NULL DEFAULT 0,            -- 命中（百分比）
    attr_penetration int NOT NULL DEFAULT 0,        -- 穿透
    attr_metal int NOT NULL DEFAULT 0,              -- 金
    attr_wood int NOT NULL DEFAULT 0,               -- 木
    attr_water int NOT NULL DEFAULT 0,              -- 水
    attr_fire int NOT NULL DEFAULT 0,               -- 火
    attr_earth int NOT NULL DEFAULT 0,              -- 土
    attr_chaos int NOT NULL DEFAULT 0               -- 混沌
);

---- 单用户单角色：同一个 user_id 只能出现一次（强约束，推荐）
--DO $$
--BEGIN
--    IF NOT EXISTS (
--        SELECT 1 FROM pg_constraint WHERE conname = 'uk_characters_user'
--    ) THEN
--        ALTER TABLE characters
--        ADD CONSTRAINT uk_characters_user UNIQUE (user_id);
--    END IF;
--END $$;

-- 如以后要开放“同一用户可多角色，但角色名不可重复”，再启用下面这个可选唯一索引（当前单角色情况下不需要）
-- CREATE UNIQUE INDEX IF NOT EXISTS uk_characters_user_name ON characters(user_id, name);

-- 常用查询索引（用户 -> 角色）
CREATE INDEX IF NOT EXISTS idx_characters_user_id ON characters(user_id);

-- ================= 元数据注释（\d+ 或 PgAdmin 查看更直观） =================
COMMENT ON TABLE characters IS '人物表：与用户弱关联（无外键），单用户单角色由 uk_characters_user 约束保证';
COMMENT ON COLUMN characters.id IS '主键：UUID，默认 gen_random_uuid() 生成';
COMMENT ON COLUMN characters.user_id IS '用户ID（弱关联，不加外键）；由 uk_characters_user 保证一个用户最多一个角色';
COMMENT ON COLUMN characters.name IS '角色名（当前单角色情况下仅作展示；如多角色再对 (user_id,name) 唯一）';
COMMENT ON COLUMN characters.exp IS '经验值（bigint，便于长线成长）';
COMMENT ON COLUMN characters.created_at IS '创建时间（带时区）';

COMMENT ON COLUMN characters.attr_attack IS '攻击';
COMMENT ON COLUMN characters.attr_atk_speed_pct IS '攻速（百分比）';
COMMENT ON COLUMN characters.attr_crit_rate_pct IS '暴击率（百分比）';
COMMENT ON COLUMN characters.attr_crit_dmg_pct IS '暴击伤害（百分比）';
COMMENT ON COLUMN characters.attr_hit_pct IS '命中（百分比）';
COMMENT ON COLUMN characters.attr_penetration IS '穿透';
COMMENT ON COLUMN characters.attr_metal IS '金';
COMMENT ON COLUMN characters.attr_wood IS '木';
COMMENT ON COLUMN characters.attr_water IS '水';
COMMENT ON COLUMN characters.attr_fire IS '火';
COMMENT ON COLUMN characters.attr_earth IS '土';
COMMENT ON COLUMN characters.attr_chaos IS '混沌';

-- 可选：为索引加注释（便于运维识别用途）
COMMENT ON INDEX idx_characters_user_id IS '按 user_id 查找角色的查询索引';
-- COMMENT ON INDEX uk_characters_user_name IS '（可选）多角色场景下限制同一用户的人物名唯一';

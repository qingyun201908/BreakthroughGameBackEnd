-- =========================================
-- Vx_create_equipment_codex_no_fk.sql
-- 装备图鉴（无外键）+ 触发器 + 中文注解
-- =========================================

-- 依赖扩展：UUID 生成
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================
-- 通用：时间戳触发器函数（用于 created_at / updated_at）
-- =========================================
CREATE OR REPLACE FUNCTION trg_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    IF NEW.created_at IS NULL THEN
      NEW.created_at := NOW();
    END IF;
    IF NEW.updated_at IS NULL THEN
      NEW.updated_at := NEW.created_at;
    END IF;
  ELSE
    NEW.updated_at := NOW();
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 工具：根据稀有度填充默认颜色（前端品质点用）
-- =========================================
CREATE OR REPLACE FUNCTION eq_color_by_rarity(p_rarity text)
RETURNS text AS $$
BEGIN
  -- 中文备注：与后端枚举一致（可按需调整）
  CASE UPPER(p_rarity)
    WHEN 'COMMON'    THEN RETURN '#9ca3af';  -- 灰
    WHEN 'UNCOMMON'  THEN RETURN '#22c55e';  -- 绿
    WHEN 'RARE'      THEN RETURN '#3b82f6';  -- 蓝
    WHEN 'EPIC'      THEN RETURN '#a855f7';  -- 紫
    WHEN 'LEGENDARY' THEN RETURN '#f59e0b';  -- 橙
    WHEN 'MYTHIC'    THEN RETURN '#ef4444';  -- 红
    ELSE RETURN '#9ca3af';
  END CASE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 触发器：在写入/更新时若 color_hex 为空则按 rarity 自动补色，并刷新时间戳
CREATE OR REPLACE FUNCTION trg_eqdef_fill_color_and_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.color_hex IS NULL OR length(trim(NEW.color_hex)) = 0 THEN
    NEW.color_hex := eq_color_by_rarity(NEW.rarity);
  END IF;

  IF TG_OP = 'INSERT' THEN
    IF NEW.created_at IS NULL THEN NEW.created_at := NOW(); END IF;
    IF NEW.updated_at IS NULL THEN NEW.updated_at := NEW.created_at; END IF;
  ELSE
    NEW.updated_at := NOW();
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================
-- 1) 装备图鉴定义（静态数据，不与人物绑定）
-- =========================================
CREATE TABLE IF NOT EXISTS equipment_definition (
    id                   uuid         PRIMARY KEY DEFAULT gen_random_uuid(),  -- 主键
    equip_key            varchar(64)  NOT NULL,                               -- 唯一键（如 glove_taiji）
    name                 varchar(64)  NOT NULL,                               -- 展示名
    slot                 varchar(24)  NOT NULL,                               -- 槽位（与前端 key 一致）
    rarity               varchar(24)  NOT NULL,                               -- 稀有度
    star_max             integer      NOT NULL DEFAULT 5,                     -- 星级上限
    level_requirement    integer      NOT NULL DEFAULT 1,                     -- 佩戴等级（展示用）
    color_hex            varchar(16)  NOT NULL DEFAULT '#9ca3af',             -- 品质色（为空时触发器按稀有度补色）
    icon                 varchar(128),                                        -- 图标资源标识
    description          varchar(512),                                        -- 描述
    sort_order           integer      NOT NULL DEFAULT 0,                     -- 排序（越小越靠前）
    version              bigint       NOT NULL DEFAULT 0,                     -- 乐观锁版本号（应用层维护）

    -- 复用 CombatAttributes：按既定列名 attr_*
    attr_attack          integer      NOT NULL DEFAULT 0,
    attr_atk_speed_pct   integer      NOT NULL DEFAULT 0,
    attr_crit_rate_pct   integer      NOT NULL DEFAULT 0,
    attr_crit_dmg_pct    integer      NOT NULL DEFAULT 0,
    attr_hit_pct         integer      NOT NULL DEFAULT 0,
    attr_penetration     integer      NOT NULL DEFAULT 0,
    attr_metal           integer      NOT NULL DEFAULT 0,
    attr_wood            integer      NOT NULL DEFAULT 0,
    attr_water           integer      NOT NULL DEFAULT 0,
    attr_fire            integer      NOT NULL DEFAULT 0,
    attr_earth           integer      NOT NULL DEFAULT 0,
    attr_chaos           integer      NOT NULL DEFAULT 0,

    enabled              boolean      NOT NULL DEFAULT true,                  -- 是否在图鉴中显示
    release_version      varchar(32)        DEFAULT '1.0.0',                  -- 投放版本
    created_at           timestamptz  NOT NULL DEFAULT NOW(),                 -- 创建时间
    updated_at           timestamptz  NOT NULL DEFAULT NOW(),                 -- 更新时间

    CONSTRAINT uk_equipment_definition_key UNIQUE (equip_key),
    CONSTRAINT ck_equipment_slot CHECK (slot IN (
      'cloak','wrist','glove','armor','belt','pants','shoes','ring','necklace','shoulder'
    )),
    CONSTRAINT ck_equipment_rarity CHECK (rarity IN (
      'COMMON','UNCOMMON','RARE','EPIC','LEGENDARY','MYTHIC'
    )),
    CONSTRAINT ck_equipment_star_max CHECK (star_max >= 0),
    CONSTRAINT ck_equipment_level_req CHECK (level_requirement >= 1),
    CONSTRAINT ck_equipment_color_hex CHECK (color_hex ~* '^#[0-9a-f]{6}$')   -- HEX 颜色校验
);

-- 索引（筛选/排序/可见性）
CREATE INDEX IF NOT EXISTS idx_eqdef_slot       ON equipment_definition (slot);
CREATE INDEX IF NOT EXISTS idx_eqdef_rarity     ON equipment_definition (rarity);
CREATE INDEX IF NOT EXISTS idx_eqdef_enabled    ON equipment_definition (enabled);
CREATE INDEX IF NOT EXISTS idx_eqdef_sort       ON equipment_definition (sort_order);
CREATE INDEX IF NOT EXISTS idx_eqdef_updated_at ON equipment_definition (updated_at);

-- 触发器：自动补色 + 更新时间
DROP TRIGGER IF EXISTS trg_equipment_definition_set_timestamp ON equipment_definition;
CREATE TRIGGER trg_equipment_definition_set_timestamp
BEFORE INSERT OR UPDATE ON equipment_definition
FOR EACH ROW EXECUTE FUNCTION trg_eqdef_fill_color_and_timestamp();

-- 注解
COMMENT ON TABLE  equipment_definition IS '装备图鉴定义（静态）：名称/槽位/稀有度/星级/属性/图标/描述/排序等（无外键）';
COMMENT ON COLUMN equipment_definition.id                IS '主键 UUID（gen_random_uuid）';
COMMENT ON COLUMN equipment_definition.equip_key         IS '装备唯一键（业务侧引用首选）';
COMMENT ON COLUMN equipment_definition.name              IS '展示名（可多语言扩展）';
COMMENT ON COLUMN equipment_definition.slot              IS '装备槽位（与前端 slots.key 对齐）';
COMMENT ON COLUMN equipment_definition.rarity            IS '稀有度：COMMON/UNCOMMON/RARE/EPIC/LEGENDARY/MYTHIC';
COMMENT ON COLUMN equipment_definition.star_max          IS '最大星级上限';
COMMENT ON COLUMN equipment_definition.level_requirement IS '佩戴等级要求（图鉴展示用）';
COMMENT ON COLUMN equipment_definition.color_hex         IS '品质色（为空时触发器按稀有度自动填充）';
COMMENT ON COLUMN equipment_definition.icon              IS '图标资源路径/键';
COMMENT ON COLUMN equipment_definition.description       IS '描述';
COMMENT ON COLUMN equipment_definition.sort_order        IS '排序（越小越靠前）';
COMMENT ON COLUMN equipment_definition.version           IS '乐观锁版本号（应用层维护）';
COMMENT ON COLUMN equipment_definition.enabled           IS '是否可见';
COMMENT ON COLUMN equipment_definition.release_version   IS '投放版本号';
COMMENT ON COLUMN equipment_definition.created_at        IS '创建时间';
COMMENT ON COLUMN equipment_definition.updated_at        IS '更新时间（触发器自动刷新）';

-- =========================================
-- 2) 标签表：与 equip_key 建立弱关联（无外键）
-- =========================================
CREATE TABLE IF NOT EXISTS equipment_definition_tag (
    equip_key  varchar(64) NOT NULL,           -- 指向 equipment_definition.equip_key（应用层保证存在性）
    tag        varchar(48) NOT NULL,           -- 标签（如 入门/拳套/太极）
    PRIMARY KEY (equip_key, tag)
);
CREATE INDEX IF NOT EXISTS idx_eqtag_tag ON equipment_definition_tag (tag);

COMMENT ON TABLE  equipment_definition_tag IS '装备图鉴标签（无外键）：(equip_key, tag) 复合主键';
COMMENT ON COLUMN equipment_definition_tag.equip_key IS '装备唯一键（弱关联，不设外键）';
COMMENT ON COLUMN equipment_definition_tag.tag       IS '标签名';

-- =========================================
-- 3) 来源副本映射：与 dungeon_key 建立弱关联（无外键）
--    对齐您的 dungeon_definition（uk_dungeondf_key）
-- =========================================
CREATE TABLE IF NOT EXISTS equipment_definition__dungeon (
    equip_key    varchar(64) NOT NULL,         -- 指向 equipment_definition.equip_key（弱关联）
    dungeon_key  varchar(64) NOT NULL,         -- 指向 dungeon_definition.dungeon_key（弱关联）
    PRIMARY KEY (equip_key, dungeon_key)
);
CREATE INDEX IF NOT EXISTS idx_eqdef_dungeon_key ON equipment_definition__dungeon (dungeon_key);

COMMENT ON TABLE  equipment_definition__dungeon IS '装备与副本来源映射（无外键）：(equip_key, dungeon_key) 复合主键';
COMMENT ON COLUMN equipment_definition__dungeon.equip_key   IS '装备唯一键（弱关联）';
COMMENT ON COLUMN equipment_definition__dungeon.dungeon_key IS '副本唯一键（弱关联）';

-- =========================================
-- 4) 示例种子：太极手套（与前端演示一致）
-- =========================================
--INSERT INTO equipment_definition (
--    equip_key, name, slot, rarity, star_max, level_requirement, color_hex, icon, description,
--    sort_order, version,
--    attr_attack, attr_atk_speed_pct, attr_crit_rate_pct, attr_crit_dmg_pct, attr_hit_pct, attr_penetration,
--    attr_metal, attr_wood, attr_water, attr_fire, attr_earth, attr_chaos,
--    enabled, release_version
--)
--VALUES (
--    'glove_taiji', '太极手套', 'glove', 'COMMON', 5, 1, NULL, 'icons/gloves/taiji.png', '入门拳法手套，蕴含太极之意。',
--    0, 0,
--    10, 0, 0, 0, 0, 0,
--     0, 0, 0, 0, 0, 0,
--    true, '1.0.0'
--)
--ON CONFLICT (equip_key) DO NOTHING;  -- 多次迁移可安全重放
--
---- 示例标签
--INSERT INTO equipment_definition_tag (equip_key, tag) VALUES
--('glove_taiji', '入门'),
--('glove_taiji', '拳套'),
--('glove_taiji', '太极')
--ON CONFLICT DO NOTHING;

-- 示例来源映射（如该装备掉落于“equip”副本；请按实际 dungeon_key 调整）
INSERT INTO equipment_definition__dungeon (equip_key, dungeon_key) VALUES
('glove_taiji', 'equip')
ON CONFLICT DO NOTHING;

-- =========================================
-- （可选）巡检 SQL：发现孤儿映射/标签（无外键场景建议定期执行）
-- =========================================
-- 1) 标签指向不存在的装备
-- SELECT t.* FROM equipment_definition_tag t
-- LEFT JOIN equipment_definition d ON d.equip_key = t.equip_key
-- WHERE d.equip_key IS NULL;

-- 2) 来源映射指向不存在的装备或副本
-- SELECT m.* FROM equipment_definition__dungeon m
-- LEFT JOIN equipment_definition d ON d.equip_key = m.equip_key
-- WHERE d.equip_key IS NULL
-- UNION ALL
-- SELECT m.* FROM equipment_definition__dungeon m
-- LEFT JOIN dungeon_definition g ON g.dungeon_key = m.dungeon_key
-- WHERE g.dungeon_key IS NULL;

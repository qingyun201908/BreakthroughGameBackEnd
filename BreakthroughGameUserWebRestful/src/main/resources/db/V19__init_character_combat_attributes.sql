-- 文件：db/migration/V20250901__init_character_combat_attributes.sql
-- 中文备注：角色-战斗属性 独立表；不加外键；一名角色一行

create table if not exists character_combat_attributes (
    id uuid primary key default gen_random_uuid(),    -- 主键
    character_id uuid not null,                       -- 角色 UUID（弱关联）

    -- 战斗属性（百分比字段以“整数百分比”表示）
    attr_attack int not null default 0,               -- 攻击（纯数值）
    attr_atk_speed_pct int not null default 0,      -- 攻速（百分比；100=100%）
    attr_crit_rate_pct int not null default 0,        -- 暴击率（百分比）
    attr_crit_dmg_pct int not null default 0,       -- 暴击伤害（百分比；150=150%）
    attr_hit_pct int not null default 0,            -- 命中（百分比；100=100%）
    attr_penetration int not null default 0,          -- 穿透（纯数值）

    attr_metal int not null default 0,                -- 金
    attr_wood int not null default 0,                 -- 木
    attr_water int not null default 0,                -- 水
    attr_fire int not null default 0,                 -- 火
    attr_earth int not null default 0,                -- 土
    attr_chaos int not null default 0,                -- 混沌

    version bigint not null default 0,                -- 乐观锁
    updated_at timestamptz not null default now()     -- 更新时间（应用层仍会覆盖）
);

-- 一名角色仅允许一行属性记录
create unique index if not exists uk_ccattrs_character_id on character_combat_attributes(character_id);

-- 常用查询索引
create index if not exists idx_ccattrs_updated_at on character_combat_attributes(updated_at);

-- ====================== 表注释 ======================
comment on table character_combat_attributes is '人物-战斗属性独立表（弱关联 character_id；一名角色仅一行）';

-- ====================== 列注释 ======================
comment on column character_combat_attributes.id                  is '主键 UUID（默认 gen_random_uuid()）';
comment on column character_combat_attributes.character_id        is '角色 UUID（弱关联，不加外键）';

comment on column character_combat_attributes.attr_attack         is '攻击（纯数值）';
comment on column character_combat_attributes.attr_atk_speed_pct  is '攻速（整数百分比；100=100% 基础攻速）';
comment on column character_combat_attributes.attr_crit_rate_pct  is '暴击率（整数百分比）';
comment on column character_combat_attributes.attr_crit_dmg_pct   is '暴击伤害（整数百分比；150=150% 基础暴伤）';
comment on column character_combat_attributes.attr_hit_pct        is '命中（整数百分比；100=100% 基础命中）';
comment on column character_combat_attributes.attr_penetration    is '穿透（纯数值）';

comment on column character_combat_attributes.attr_metal          is '五行：金（纯数值）';
comment on column character_combat_attributes.attr_wood           is '五行：木（纯数值）';
comment on column character_combat_attributes.attr_water          is '五行：水（纯数值）';
comment on column character_combat_attributes.attr_fire           is '五行：火（纯数值）';
comment on column character_combat_attributes.attr_earth          is '五行：土（纯数值）';
comment on column character_combat_attributes.attr_chaos          is '混沌（纯数值）';

comment on column character_combat_attributes.version             is '乐观锁版本号';
comment on column character_combat_attributes.updated_at          is '记录更新时间（由应用层 @PrePersist/@PreUpdate 维护）';

-- ====================== 索引注释（可选） ======================
comment on index uk_ccattrs_character_id is '唯一索引：保证每个 character_id 仅一行战斗属性';
comment on index idx_ccattrs_updated_at  is '查询索引：按 updated_at 排序/过滤';

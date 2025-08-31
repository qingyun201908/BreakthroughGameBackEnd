-- 文件：db/migration/V20250828__init_character_bag.sql
-- 目的：初始化“人物背包”相关表（meta + item），采用 UUID 主键与乐观锁版本号
-- 说明：
--   1) 与人物表为“弱关联”：只保存 character_id（UUID），不建外键，避免跨库/耦合
--   2) 对应前端 InventoryPage.vue 的数据结构：capacity + items[{id,name,qty,type,rarity,desc}]
--   3) 默认行为：删除至 0 数量的栈可由服务层决定是否物理删除（见后端实现）
--   4) 本脚本使用 gen_random_uuid()，请确保启用 pgcrypto 扩展

-- ========== 依赖扩展 ==========
-- 中文备注：gen_random_uuid() 来源于 pgcrypto；若已启用可重复执行，不会报错
create extension if not exists pgcrypto;

-- ========== 表：character_bag_meta（人物背包元信息） ==========
create table if not exists character_bag_meta (
    id            uuid primary key default gen_random_uuid(),     -- 主键：UUID
    character_id  uuid not null,                                  -- 人物 UUID（弱关联：无外键）
    capacity      int  not null default 24,                       -- 背包容量（格子数）
    version       bigint not null default 0,                      -- 乐观锁版本号（与 JPA @Version 对应）
    updated_at    timestamptz not null default now(),             -- 记录更新时间（可选触发器自动刷新）
    -- 约束：容量必须 >= 1（如需上限可加 AND capacity <= 200）
    constraint ck_bagmeta_capacity check (capacity >= 1)
);

create unique index if not exists uk_bagmeta_character_id on character_bag_meta(character_id);
create index if not exists idx_bagmeta_updated_at on character_bag_meta(updated_at);

-- 元数据注释：表与列（COMMENT ON 在 psql \d+ 可见）
comment on table  character_bag_meta                       is '人物背包元信息：每个角色 1 行，主要维护 capacity';
comment on column character_bag_meta.id                    is '主键 UUID';
comment on column character_bag_meta.character_id          is '人物 UUID，仅存引用，不建外键';
comment on column character_bag_meta.capacity              is '背包容量（格子数），默认 24';
comment on column character_bag_meta.version               is '乐观锁版本号，对应 JPA @Version';
comment on column character_bag_meta.updated_at            is '更新时间戳，默认 now()；可用触发器自动刷新';

-- ========== 表：character_bag_item（人物背包条目/栈） ==========
create table if not exists character_bag_item (
    id            uuid         primary key default gen_random_uuid(),   -- 主键：UUID
    character_id  uuid         not null,                                -- 人物 UUID（弱关联：无外键）
    item_key      varchar(64)  not null,                                -- 物品唯一码（用于同类叠加）
    name          varchar(64)  not null,                                -- 展示名称（冗余，方便列表展示/搜索）
    type          varchar(16)  not null,                                -- 物品类型：consumable/equipment/material/quest/misc
    rarity        int          not null default 1,                      -- 稀有度 1~5
    qty           int          not null default 0,                      -- 栈内数量（>=0）
    "desc"        text,                                                 -- 描述（冗余，便于详情）
    version       bigint       not null default 0,                      -- 乐观锁版本号
    updated_at    timestamptz  not null default now(),                  -- 更新时间
    -- 约束：数量 >= 0，稀有度 1~5，类型白名单
    constraint ck_bagitem_qty    check (qty >= 0),
    constraint ck_bagitem_rarity check (rarity between 1 and 5),
    constraint ck_bagitem_type   check (type in ('consumable','equipment','material','quest','misc'))
);

-- 同角色下每个 item_key 仅一条记录（用于叠加栈）
create unique index if not exists uk_bagitem_char_itemkey on character_bag_item(character_id, item_key);

-- 常用检索索引
create index if not exists idx_bagitem_character_id on character_bag_item(character_id); -- 角色维度查询
create index if not exists idx_bagitem_type         on character_bag_item(type);         -- 类型筛选
create index if not exists idx_bagitem_rarity       on character_bag_item(rarity);       -- 稀有度排序/筛选
create index if not exists idx_bagitem_updated_at   on character_bag_item(updated_at);   -- 最近更新排序
create index if not exists idx_bagitem_name         on character_bag_item(name);         -- 名称前缀搜索（LIKE 'kw%'）

-- 元数据注释：表与列
comment on table  character_bag_item                      is '人物背包条目（按 item_key 叠加为单栈）';
comment on column character_bag_item.id                   is '主键 UUID';
comment on column character_bag_item.character_id         is '人物 UUID，仅存引用，不建外键';
comment on column character_bag_item.item_key             is '物品唯一键（掉落/策划定义），用于同种叠加';
comment on column character_bag_item.name                 is '物品名称（冗余字段，提升列表查询效率）';
comment on column character_bag_item.type                 is '物品类型（consumable/equipment/material/quest/misc）';
comment on column character_bag_item.rarity               is '稀有度（1~5），默认 1';
comment on column character_bag_item.qty                  is '物品数量（栈内叠加，>=0）';
comment on column character_bag_item."desc"               is '物品描述（冗余，方便详情展示）';
comment on column character_bag_item.version              is '乐观锁版本号，对应 JPA @Version';
comment on column character_bag_item.updated_at           is '更新时间戳，默认 now()；可用触发器自动刷新';

-- ========== 可选增强：updated_at 自动更新时间触发器 ==========
-- 中文备注：如需在 INSERT/UPDATE 时自动刷新 updated_at，可启用以下函数与触发器
-- 优点：避免忘记在应用层手动改 updated_at；缺点：每次写入会多一次触发器开销
-- 使用方式：取消注释以下代码块

-- create or replace function set_updated_at() returns trigger as $$
-- begin
--   new.updated_at := now(); -- 中文备注：每次写入自动刷新
--   return new;
-- end;
-- $$ language plpgsql;

-- -- 触发器：character_bag_meta
-- drop trigger if exists trg_set_updated_at_bag_meta on character_bag_meta;
-- create trigger trg_set_updated_at_bag_meta
--   before insert or update on character_bag_meta
--   for each row execute function set_updated_at();

-- -- 触发器：character_bag_item
-- drop trigger if exists trg_set_updated_at_bag_item on character_bag_item;
-- create trigger trg_set_updated_at_bag_item
--   before insert or update on character_bag_item
--   for each row execute function set_updated_at();

-- ========== 可选增强：名称模糊搜索性能（pg_trgm） ==========
-- 中文备注：若前端使用 LIKE '%kw%' 搜索 name，建议启用 pg_trgm + GIN 索引
-- create extension if not exists pg_trgm;
-- create index if not exists gin_bagitem_name_trgm on character_bag_item using gin (name gin_trgm_ops);

-- ========== 示例数据（可选） ==========
-- insert into character_bag_meta(character_id, capacity)
-- values ('11111111-1111-1111-1111-111111111111', 24);

-- insert into character_bag_item(character_id,item_key,name,type,rarity,qty,"desc")
-- values ('11111111-1111-1111-1111-111111111111','i1','药水','consumable',2,5,'恢复少量生命。');

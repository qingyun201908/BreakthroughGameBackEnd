-- 文件：db/migration/V20250831__init_character_equipment.sql
-- 中文备注：人物-装备当前穿戴表（无外键）；一行代表角色在某个槽位当前穿着的 1 件装备
create table if not exists character_equipment (
    id uuid primary key default gen_random_uuid(),          -- 主键
    character_id uuid not null,                             -- 角色 UUID（弱关联）
    slot varchar(24) not null,                              -- 槽位（存枚举名，例如 CLOAK/WRIST...）
    item_key varchar(64) not null,                          -- 图鉴 key（弱关联 equipment_definition.equip_key）
    name varchar(64) not null,                              -- 冗余：装备名（方便前端直接展示）
    rarity int not null default 1,                          -- 冗余：稀有度
    icon varchar(256),                                      -- 冗余：图标 URL（可选）
    description text,                                       -- 冗余：描述（可选）
    bag_item_id uuid,                                       -- 冗余：来源的背包条目 ID（弱关联，可空）
    version bigint not null default 0,                      -- 乐观锁
    updated_at timestamptz not null default now()
);
create unique index if not exists uk_chequip_character_slot on character_equipment(character_id, slot);
create index if not exists idx_chequip_character_id on character_equipment(character_id);
create index if not exists idx_chequip_item_key on character_equipment(item_key);
create index if not exists idx_chequip_updated_at on character_equipment(updated_at);

-- 中文备注：人物-装备变更历史表（审计）；记录每次穿/卸/替换
create table if not exists character_equipment_history (
    id uuid primary key default gen_random_uuid(),
    character_id uuid not null,
    slot varchar(24) not null,
    old_item_key varchar(64),
    new_item_key varchar(64),
    reason varchar(64) not null,                 -- 例如：EQUIP / UNEQUIP / SWAP
    trace_id varchar(64),                        -- 业务链路追踪（可从 MDC 注入）
    created_at timestamptz not null default now()
);
create index if not exists idx_chequip_his_character_id on character_equipment_history(character_id);
create index if not exists idx_chequip_his_created_at on character_equipment_history(created_at);

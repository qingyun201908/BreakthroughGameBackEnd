-- ============================
-- 注释：人物-装备当前穿戴
-- ============================

COMMENT ON TABLE public.character_equipment
  IS '人物-装备当前穿戴（无外键）：一行代表角色在某个槽位当前穿着的 1 件装备；同一角色同槽位唯一';

COMMENT ON COLUMN public.character_equipment.id
  IS '主键 UUID';
COMMENT ON COLUMN public.character_equipment.character_id
  IS '角色 UUID（弱关联，不建外键）';
COMMENT ON COLUMN public.character_equipment.slot
  IS '槽位（存枚举名，如 CLOAK/WRIST/...，与前端 slots.key 对齐）';
COMMENT ON COLUMN public.character_equipment.item_key
  IS '图鉴 equip_key（弱关联 equipment_definition.equip_key）';
COMMENT ON COLUMN public.character_equipment.name
  IS '冗余：装备名（便于直接展示/列表查询）';
COMMENT ON COLUMN public.character_equipment.rarity
  IS '冗余：稀有度（建议 1~5）';
COMMENT ON COLUMN public.character_equipment.icon
  IS '冗余：图标 URL/资源键（可空）';
COMMENT ON COLUMN public.character_equipment.description
  IS '冗余：描述（可空）';
COMMENT ON COLUMN public.character_equipment.bag_item_id
  IS '冗余：来源背包条目 ID（便于追溯审计，可空）';
COMMENT ON COLUMN public.character_equipment.version
  IS '乐观锁版本号（对应 JPA @Version）';
COMMENT ON COLUMN public.character_equipment.updated_at
  IS '更新时间戳（默认 now()，可由触发器自动刷新）';

-- 索引注释（可选）
COMMENT ON INDEX public.uk_chequip_character_slot
  IS '唯一约束：同一角色在一个槽位最多穿 1 件装备';
COMMENT ON INDEX public.idx_chequip_character_id
  IS '查询优化：按角色维度检索当前穿戴';
COMMENT ON INDEX public.idx_chequip_item_key
  IS '查询优化：按 equip_key 检索穿戴分布';
COMMENT ON INDEX public.idx_chequip_updated_at
  IS '查询优化：按更新时间排序/清理';

-- ============================
-- 注释：人物-装备变更历史（审计）
-- ============================

COMMENT ON TABLE public.character_equipment_history
  IS '人物-装备变更历史（审计表）：记录每次穿/卸/替换，便于追踪与统计';

COMMENT ON COLUMN public.character_equipment_history.id
  IS '主键 UUID';
COMMENT ON COLUMN public.character_equipment_history.character_id
  IS '角色 UUID（弱关联，不建外键）';
COMMENT ON COLUMN public.character_equipment_history.slot
  IS '槽位（枚举名，与当前穿戴表一致）';
COMMENT ON COLUMN public.character_equipment_history.old_item_key
  IS '变更前 equip_key（首次穿戴时为空）';
COMMENT ON COLUMN public.character_equipment_history.new_item_key
  IS '变更后 equip_key（卸下时为空）';
COMMENT ON COLUMN public.character_equipment_history.reason
  IS '变更原因：EQUIP/UNEQUIP/SWAP 等';
COMMENT ON COLUMN public.character_equipment_history.trace_id
  IS '链路追踪 ID（可由 MDC 注入，便于排查）';
COMMENT ON COLUMN public.character_equipment_history.created_at
  IS '记录创建时间（默认 now()）';

-- 索引注释（可选）
COMMENT ON INDEX public.idx_chequip_his_character_id
  IS '查询优化：按角色维度检索历史';
COMMENT ON INDEX public.idx_chequip_his_created_at
  IS '查询优化：按时间维度排序/归档';

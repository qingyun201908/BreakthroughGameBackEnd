-- V11__add_day_key.sql

-- 1) 增加 day_key 为“存储生成列”，由 created_at 在 Asia/Singapore 时区换算而来
--    PostgreSQL 12+ 支持 GENERATED ALWAYS AS (...) STORED
ALTER TABLE dungeon_run_log
    ADD COLUMN day_key date
    GENERATED ALWAYS AS ((created_at AT TIME ZONE 'Asia/Singapore')::date) STORED;

-- 2) 索引：按你的查找条件合并成复合索引，查询会非常快
CREATE INDEX IF NOT EXISTS idx_dgrunlog_cid_dkey_day_trace
    ON dungeon_run_log (character_id, dungeon_key, day_key, trace_id);

-- 可选：如果需要强幂等（同一组合唯一），可加唯一索引：
-- ⚠️ 若历史数据可能有重复，先去重再执行，否则这里会失败。
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_dgrunlog_cid_dkey_day_trace
--     ON dungeon_run_log (character_id, dungeon_key, day_key, trace_id);

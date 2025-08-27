-- 文件名建议：V20250826_01__sync_daily_to_clear.sql
-- 中文备注：将 character_dungeon_daily 的更新，自动同步到 character_dungeon_clear

-- =========================
-- 1) 辅助函数：UPSERT 终身通关进度
-- =========================
CREATE OR REPLACE FUNCTION fn_upsert_dungeon_clear(
    p_character_id          uuid,
    p_dungeon_key           varchar,
    p_cleared_difficulty    integer,
    p_cleared_at            timestamptz DEFAULT now()
)
RETURNS void
LANGUAGE plpgsql
SET search_path = public
AS $$
DECLARE
BEGIN
    -- 中文备注：简单校验（可按需放宽/去除）
    IF p_cleared_difficulty IS NULL OR p_cleared_difficulty < 0 THEN
        RAISE EXCEPTION 'cleared_difficulty 无效: %', p_cleared_difficulty;
    END IF;

    -- 中文备注：并发安全的 UPSERT 模式（先尝试 UPDATE，若无行再 INSERT，若撞唯一约束则重试）
    LOOP
        -- 优先更新已存在记录
        UPDATE character_dungeon_clear c
           SET highest_cleared_difficulty = GREATEST(c.highest_cleared_difficulty, p_cleared_difficulty),  -- 中文备注：提高最高难度
               first_cleared_at = CASE
                   WHEN p_cleared_difficulty > c.highest_cleared_difficulty THEN p_cleared_at              -- 中文备注：首次达到新的更高难度
                   ELSE c.first_cleared_at
               END,
               -- 中文备注：仅当本次难度 >= 历史最高时，刷新最近一次时间；否则保持不变（可按需调整策略）
               last_cleared_at = CASE
                   WHEN p_cleared_difficulty >= c.highest_cleared_difficulty THEN p_cleared_at
                   ELSE c.last_cleared_at
               END,
               version    = CASE
                   WHEN p_cleared_difficulty > c.highest_cleared_difficulty THEN c.version + 1
                   ELSE c.version
               END,
               updated_at = now()
         WHERE c.character_id = p_character_id
           AND c.dungeon_key  = p_dungeon_key;

        IF FOUND THEN
            RETURN;  -- 中文备注：已更新则结束
        END IF;

        -- 不存在则插入新记录
        BEGIN
            INSERT INTO character_dungeon_clear (
                character_id, dungeon_key, highest_cleared_difficulty,
                first_cleared_at, last_cleared_at, version, updated_at
            ) VALUES (
                p_character_id, p_dungeon_key, GREATEST(0, p_cleared_difficulty),
                p_cleared_at, p_cleared_at, 0, now()
            );
            RETURN;
        EXCEPTION WHEN unique_violation THEN
            -- 中文备注：并发下可能被别的事务先插入了，循环重试 UPDATE
        END;
    END LOOP;
END;
$$;

-- =========================
-- 2) 触发器函数：当日计数表 -> 终身通关表
-- =========================
CREATE OR REPLACE FUNCTION trg_chardgdaily__sync_clear()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
DECLARE
    _delta int;  -- 中文备注：本次 runs_used 增量
BEGIN
    -- 中文备注：只在“新增”或“runs_used 增加”时触发同步
    IF TG_OP = 'INSERT' THEN
        _delta := COALESCE(NEW.runs_used, 0);
    ELSE
        _delta := GREATEST(0, COALESCE(NEW.runs_used, 0) - COALESCE(OLD.runs_used, 0));
    END IF;

    -- 中文备注：若本次确有消耗次数，视为通关一次当前选择的难度
    IF _delta > 0 AND COALESCE(NEW.last_selected_difficulty, 0) >= 1 THEN
        PERFORM fn_upsert_dungeon_clear(
            NEW.character_id,
            NEW.dungeon_key,
            NEW.last_selected_difficulty,
            now()
        );
    END IF;

    RETURN NEW;
END;
$$;

-- =========================
-- 3) 绑定触发器到当日计数表
-- =========================
DROP TRIGGER IF EXISTS chardgdaily_sync_clear ON character_dungeon_daily;

CREATE TRIGGER chardgdaily_sync_clear
AFTER INSERT OR UPDATE OF runs_used, last_selected_difficulty
ON character_dungeon_daily
FOR EACH ROW
EXECUTE FUNCTION trg_chardgdaily__sync_clear();

-- 可选：也可增加一个 BEFORE UPDATE 触发器，自动维护 updated_at（如果需要）
-- 但这里我们在 UPSERT 时已手动维护 clear 表的 updated_at，daily 表已默认 now()。

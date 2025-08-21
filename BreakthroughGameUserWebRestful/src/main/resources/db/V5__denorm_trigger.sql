-- 统一参数（如需改为配置表可看文末“进阶：参数表”）
CREATE OR REPLACE FUNCTION trg_character_xp_denorm_all()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  v_max_level  INT := 80;
  v_base       DOUBLE PRECISION := 1.0;
  v_scale      DOUBLE PRECISION := 8.0;
  v_soft       DOUBLE PRECISION := 1200.0;

  v_lvl   INT;
  v_start BIGINT;
  v_next  BIGINT;
  v_need  BIGINT;
BEGIN
  -- 计算等级
  v_lvl := log_level_discrete(NEW.total_xp, v_max_level, v_base, v_scale, v_soft);
  NEW.current_level := v_lvl;

  -- 计算阈值
  v_start := log_total_xp_to_reach_level(v_lvl, v_base, v_scale, v_soft);
  IF v_lvl >= v_max_level THEN
    v_next := v_start; -- 满级：下一级阈值不再增长
  ELSE
    v_next := log_total_xp_to_reach_level(v_lvl + 1, v_base, v_scale, v_soft);
  END IF;

  NEW.level_start_total_xp := v_start;
  NEW.next_level_total_xp  := v_next;

  -- 已获/剩余与进度
  v_need := GREATEST(0, v_next - v_start);
  NEW.xp_into_level  := GREATEST(0, NEW.total_xp - v_start);
  NEW.xp_to_next_level := CASE WHEN v_lvl >= v_max_level THEN 0 ELSE GREATEST(0, v_next - NEW.total_xp) END;
  NEW.is_max_level := (v_lvl >= v_max_level);
  NEW.progress :=
    CASE
      WHEN NEW.is_max_level THEN 1.0
      WHEN v_need <= 0 THEN 1.0
      ELSE LEAST(1.0, GREATEST(0.0, (NEW.total_xp - v_start)::double precision / v_need::double precision))
    END;

  -- 变动追踪
  IF TG_OP = 'INSERT' THEN
    NEW.last_delta_xp := NEW.total_xp;
    NEW.last_gain_at  := now();
  ELSIF TG_OP = 'UPDATE' THEN
    IF NEW.total_xp IS DISTINCT FROM OLD.total_xp THEN
      NEW.last_delta_xp := NEW.total_xp - OLD.total_xp;
      NEW.last_gain_at  := now();
    END IF;
  END IF;

  -- 更新时间统一维护
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

-- 绑定触发器（插入或 total_xp 更新时生效）
DROP TRIGGER IF EXISTS biu_character_xp_denorm_all ON character_xp;
CREATE TRIGGER biu_character_xp_denorm_all
BEFORE INSERT OR UPDATE OF total_xp ON character_xp
FOR EACH ROW
EXECUTE FUNCTION trg_character_xp_denorm_all();

-- 对现有数据回填（新库可忽略）
UPDATE character_xp SET total_xp = total_xp;

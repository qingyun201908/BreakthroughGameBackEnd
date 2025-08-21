-- 连续等级：base + scale * ln(1 + xp/soft)
CREATE OR REPLACE FUNCTION log_level_continuous(
  total_xp BIGINT, base DOUBLE PRECISION, scale DOUBLE PRECISION, soft DOUBLE PRECISION
) RETURNS DOUBLE PRECISION
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$ SELECT base + scale * LN(1.0 + (GREATEST(total_xp,0)::DOUBLE PRECISION / soft)); $$;

-- 离散等级：floor 并 clamp 到 [floor(base), maxLevel]
CREATE OR REPLACE FUNCTION log_level_discrete(
  total_xp BIGINT, max_level INT, base DOUBLE PRECISION, scale DOUBLE PRECISION, soft DOUBLE PRECISION
) RETURNS INT
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$
SELECT LEAST(
  max_level,
  GREATEST(
    FLOOR(log_level_continuous(total_xp, base, scale, soft))::INT,
    FLOOR(base)::INT
  )
);
$$;

-- 反函数：达到等级 L 的累计阈值经验
CREATE OR REPLACE FUNCTION log_total_xp_to_reach_level(
  lvl INT, base DOUBLE PRECISION, scale DOUBLE PRECISION, soft DOUBLE PRECISION
) RETURNS BIGINT
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$
SELECT CASE WHEN lvl <= FLOOR(base)::INT
            THEN 0::BIGINT
            ELSE FLOOR( soft * (EXP( (lvl - base) / scale ) - 1.0) + 0.5 )::BIGINT
       END;
$$;

-- 本级进度（0~1，满级恒为1）
CREATE OR REPLACE FUNCTION log_level_progress(
  total_xp BIGINT, max_level INT, base DOUBLE PRECISION, scale DOUBLE PRECISION, soft DOUBLE PRECISION
) RETURNS DOUBLE PRECISION
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$
WITH L AS (
  SELECT log_level_discrete(total_xp, max_level, base, scale, soft) AS lvl
),
S AS (
  SELECT log_total_xp_to_reach_level((SELECT lvl FROM L), base, scale, soft) AS start_xp
),
N AS (
  SELECT GREATEST(
           0,
           log_total_xp_to_reach_level((SELECT lvl FROM L)+1, base, scale, soft)
           - (SELECT start_xp FROM S)
         ) AS need_xp
)
SELECT CASE
         WHEN (SELECT lvl FROM L) >= max_level THEN 1.0
         WHEN (SELECT need_xp FROM N) <= 0     THEN 1.0
         ELSE LEAST(
                1.0,
                GREATEST(
                  0.0,
                  (GREATEST(total_xp,0)::DOUBLE PRECISION - (SELECT start_xp FROM S))
                  / NULLIF((SELECT need_xp FROM N),0)
                )
              )
       END;
$$;

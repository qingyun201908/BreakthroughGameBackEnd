CREATE OR REPLACE VIEW v_character_level AS
SELECT
  c.id AS character_id,
  COALESCE(x.total_xp, 0) AS total_xp,
  COALESCE(x.current_level, 1) AS level,
  COALESCE(x.progress, 0.0) AS progress
FROM characters c
LEFT JOIN character_xp x ON x.character_id = c.id;

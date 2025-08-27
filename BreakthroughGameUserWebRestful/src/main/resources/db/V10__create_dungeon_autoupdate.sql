CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_chardgdaily_set_updated ON character_dungeon_daily;
CREATE TRIGGER trg_chardgdaily_set_updated
  BEFORE UPDATE ON character_dungeon_daily
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

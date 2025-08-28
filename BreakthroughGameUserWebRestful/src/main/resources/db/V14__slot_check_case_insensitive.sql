DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'equipment_definition'
  ) THEN
    BEGIN
      EXECUTE 'ALTER TABLE public.equipment_definition DROP CONSTRAINT IF EXISTS ck_equipment_slot';
    EXCEPTION WHEN others THEN
      -- 忽略不存在/命名不一致等异常
      NULL;
    END;

    EXECUTE $sql$
      ALTER TABLE public.equipment_definition
      ADD CONSTRAINT ck_equipment_slot
      CHECK (lower(slot) IN ('cloak','wrist','glove','armor','belt','pants','shoes','ring','necklace','shoulder'))
    $sql$;
  END IF;
END $$;

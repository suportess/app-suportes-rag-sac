-- Registros existentes sao todos criterios que a IA retornou (sempre aplicaveis).
ALTER TABLE checklist_items ADD COLUMN IF NOT EXISTS aplicavel BOOLEAN NOT NULL DEFAULT TRUE;

-- Pressione: Farmaci (medication tracking)
-- Traccia i farmaci assunti dal paziente (nome, dosaggio, frequenza,
-- date inizio/fine, note) — allineata alla feature presente nell'app Android.
CREATE TABLE IF NOT EXISTS medications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    name TEXT NOT NULL,
    dosage TEXT DEFAULT '',
    frequency TEXT DEFAULT '',
    notes TEXT DEFAULT '',
    start_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    end_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_medications_username ON medications(username, start_date DESC);
-- RLS
ALTER TABLE medications ENABLE ROW LEVEL SECURITY;
CREATE POLICY "medications_select_own" ON medications FOR
SELECT USING (true);
CREATE POLICY "medications_insert_own" ON medications FOR
INSERT WITH CHECK (true);
CREATE POLICY "medications_update_own" ON medications FOR
UPDATE USING (true);
CREATE POLICY "medications_delete_own" ON medications FOR DELETE USING (true);
GRANT ALL ON medications TO anon,
    authenticated;
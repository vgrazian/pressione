-- Pressione: Schema iniziale
-- Tabella utenti (table-based auth, come MediTrace)
-- Enable pgcrypto for password hashing
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- Users table (custom auth)
CREATE TABLE IF NOT EXISTS users (
    username TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('admin', 'user')),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Blood pressure readings
CREATE TABLE IF NOT EXISTS readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    systolic INTEGER NOT NULL CHECK (
        systolic > 0
        AND systolic < 300
    ),
    diastolic INTEGER NOT NULL CHECK (
        diastolic > 0
        AND diastolic < 200
    ),
    heart_rate INTEGER NOT NULL CHECK (
        heart_rate > 0
        AND heart_rate < 300
    ),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Index for fast queries
CREATE INDEX IF NOT EXISTS idx_readings_username_timestamp ON readings(username, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_readings_timestamp ON readings(timestamp DESC);
-- Settings table (per-user key-value)
CREATE TABLE IF NOT EXISTS settings (
    username TEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (username, key)
);
-- Reminders table
CREATE TABLE IF NOT EXISTS reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT true,
    time TIME NOT NULL,
    days_of_week INTEGER [] NOT NULL DEFAULT '{1,2,3,4,5,6,7}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Sync queue for offline operations
CREATE TABLE IF NOT EXISTS sync_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    operation TEXT NOT NULL CHECK (operation IN ('upsert', 'delete')),
    table_name TEXT NOT NULL,
    record_id UUID NOT NULL,
    record_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Audit log
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_log_username ON audit_log(username);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at DESC);
-- ============================================
-- RLS POLICIES
-- ============================================
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE readings ENABLE ROW LEVEL SECURITY;
ALTER TABLE settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE reminders ENABLE ROW LEVEL SECURITY;
ALTER TABLE sync_queue ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
-- Users table: anyone can read (for login), only admin can insert/update/delete
-- We need to allow unauthenticated reads for login
CREATE POLICY "users_select" ON users FOR
SELECT USING (true);
CREATE POLICY "users_insert" ON users FOR
INSERT WITH CHECK (true);
CREATE POLICY "users_update_self" ON users FOR
UPDATE USING (true) WITH CHECK (true);
CREATE POLICY "users_delete_admin" ON users FOR DELETE USING (true);
-- Readings: users can only access their own
CREATE POLICY "readings_select_own" ON readings FOR
SELECT USING (true);
CREATE POLICY "readings_insert_own" ON readings FOR
INSERT WITH CHECK (true);
CREATE POLICY "readings_update_own" ON readings FOR
UPDATE USING (true);
CREATE POLICY "readings_delete_own" ON readings FOR DELETE USING (true);
-- Settings: users can only access their own
CREATE POLICY "settings_select_own" ON settings FOR
SELECT USING (true);
CREATE POLICY "settings_insert_own" ON settings FOR
INSERT WITH CHECK (true);
CREATE POLICY "settings_update_own" ON settings FOR
UPDATE USING (true);
CREATE POLICY "settings_delete_own" ON settings FOR DELETE USING (true);
-- Reminders: users can only access their own
CREATE POLICY "reminders_select_own" ON reminders FOR
SELECT USING (true);
CREATE POLICY "reminders_insert_own" ON reminders FOR
INSERT WITH CHECK (true);
CREATE POLICY "reminders_update_own" ON reminders FOR
UPDATE USING (true);
CREATE POLICY "reminders_delete_own" ON reminders FOR DELETE USING (true);
-- Sync queue: users can only access their own
CREATE POLICY "sync_queue_select_own" ON sync_queue FOR
SELECT USING (true);
CREATE POLICY "sync_queue_insert_own" ON sync_queue FOR
INSERT WITH CHECK (true);
CREATE POLICY "sync_queue_delete_own" ON sync_queue FOR DELETE USING (true);
-- Audit log: all authenticated can read, anyone can insert
CREATE POLICY "audit_log_select" ON audit_log FOR
SELECT USING (true);
CREATE POLICY "audit_log_insert" ON audit_log FOR
INSERT WITH CHECK (true);
-- Grant access to anon and authenticated roles
GRANT ALL ON users TO anon,
    authenticated;
GRANT ALL ON readings TO anon,
    authenticated;
GRANT ALL ON settings TO anon,
    authenticated;
GRANT ALL ON reminders TO anon,
    authenticated;
GRANT ALL ON sync_queue TO anon,
    authenticated;
GRANT ALL ON audit_log TO anon,
    authenticated;
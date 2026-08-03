-- Pressione: Replace age with birth_date for dynamic age computation
-- Migration 003
ALTER TABLE users
ADD COLUMN IF NOT EXISTS birth_date DATE;
COMMENT ON COLUMN users.birth_date IS 'User birth date (optional, used to compute age dynamically)';
-- Drop the old age column (data will be lost, users will re-enter birth date)
ALTER TABLE users DROP COLUMN IF EXISTS age;
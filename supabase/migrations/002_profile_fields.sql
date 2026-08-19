-- Pressione: Add profile fields (gender, profile_completed) to users table
-- Migration 002
ALTER TABLE users
ADD COLUMN IF NOT EXISTS gender TEXT CHECK (
        gender IS NULL
        OR gender IN ('male', 'female', 'other')
    );
ALTER TABLE users
ADD COLUMN IF NOT EXISTS profile_completed BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users
ADD COLUMN IF NOT EXISTS skip_profile_prompt BOOLEAN NOT NULL DEFAULT false;
COMMENT ON COLUMN users.gender IS 'male, female, or other (optional)';
COMMENT ON COLUMN users.profile_completed IS 'Whether user has completed optional profile';
COMMENT ON COLUMN users.skip_profile_prompt IS 'User chose to skip the profile prompt permanently';
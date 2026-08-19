-- Pressione: extended user profile fields (anagrafica)
-- Migration 007 — allinea il profilo utente alla web app (codice fiscale,
-- indirizzo). first_name/last_name/phone sono già nella migration 001.
ALTER TABLE users
ADD COLUMN IF NOT EXISTS fiscal_code TEXT;
ALTER TABLE users
ADD COLUMN IF NOT EXISTS street TEXT;
ALTER TABLE users
ADD COLUMN IF NOT EXISTS street_number TEXT;
ALTER TABLE users
ADD COLUMN IF NOT EXISTS city TEXT;
ALTER TABLE users
ADD COLUMN IF NOT EXISTS postal_code TEXT;
COMMENT ON COLUMN users.fiscal_code IS 'Optional fiscal code';
COMMENT ON COLUMN users.street IS 'Optional street address';
COMMENT ON COLUMN users.street_number IS 'Optional street number';
COMMENT ON COLUMN users.city IS 'Optional city';
COMMENT ON COLUMN users.postal_code IS 'Optional postal code';
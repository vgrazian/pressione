-- Pressione: Add active ingredient to medications
-- Allows recording both the product name and the active ingredient.
ALTER TABLE medications
ADD COLUMN IF NOT EXISTS active_ingredient TEXT DEFAULT '';

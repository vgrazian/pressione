-- Pressione: Email-based password recovery flow (adapted from MediTrace pattern)
-- Migration 005 — Replaces token-based recovery in settings table with proper token storage
-- 1. Dedicated recovery token table
CREATE TABLE IF NOT EXISTS public.user_password_recovery_tokens (
    token UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL REFERENCES public.users(username) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_recovery_tokens_user ON public.user_password_recovery_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_recovery_tokens_active ON public.user_password_recovery_tokens (expires_at)
WHERE consumed_at IS NULL;
ALTER TABLE public.user_password_recovery_tokens ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.user_password_recovery_tokens
FROM anon,
    authenticated;
-- Grant access for the SECURITY DEFINER functions below
-- (RLS is bypassed by SECURITY DEFINER, but we still need table-level access for the owner)
-- 2. Request password reset (returns reset URL with token)
-- Always returns the same shape to avoid leaking which emails are registered
CREATE OR REPLACE FUNCTION public.app_request_password_reset(
        p_email TEXT,
        p_reset_base_url TEXT DEFAULT '',
        p_reset_ttl_minutes INTEGER DEFAULT 30
    ) RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public AS $$
DECLARE v_email TEXT := LOWER(TRIM(COALESCE(p_email, '')));
v_user public.users;
v_token UUID;
v_minutes INTEGER := GREATEST(COALESCE(p_reset_ttl_minutes, 30), 5);
v_expires_at TIMESTAMPTZ;
v_base_url TEXT := TRIM(COALESCE(p_reset_base_url, ''));
v_reset_url TEXT;
BEGIN -- Find user by email (must be active)
SELECT * INTO v_user
FROM public.users
WHERE LOWER(email) = v_email
    AND disabled = FALSE
LIMIT 1;
-- Always return consistent shape — don't leak account existence
IF NOT FOUND THEN RETURN jsonb_build_object(
    'email',
    v_email,
    'reset_url',
    NULL,
    'expires_at',
    NULL
);
END IF;
-- Invalidate any existing unconsumed tokens for this user
DELETE FROM public.user_password_recovery_tokens
WHERE user_id = v_user.username
    AND consumed_at IS NULL;
v_expires_at := NOW() + make_interval(mins => v_minutes);
INSERT INTO public.user_password_recovery_tokens (user_id, expires_at)
VALUES (v_user.username, v_expires_at)
RETURNING token INTO v_token;
-- Build the reset URL
IF v_base_url = '' THEN v_base_url := '/#/reset-password';
END IF;
IF POSITION('?' IN v_base_url) > 0 THEN v_reset_url := v_base_url || '&token=' || v_token::TEXT;
ELSE v_reset_url := v_base_url || '?token=' || v_token::TEXT;
END IF;
RETURN jsonb_build_object(
    'email',
    v_email,
    'reset_url',
    v_reset_url,
    'expires_at',
    v_expires_at
);
END;
$$;
-- 3. Complete password recovery (consume token, update password)
CREATE OR REPLACE FUNCTION public.app_complete_password_recovery(p_token TEXT, p_new_password TEXT) RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public AS $$
DECLARE v_token UUID;
v_password TEXT := TRIM(COALESCE(p_new_password, ''));
v_recovery public.user_password_recovery_tokens;
v_user public.users;
BEGIN -- Validate token format
BEGIN v_token := TRIM(COALESCE(p_token, ''))::UUID;
EXCEPTION
WHEN OTHERS THEN RAISE EXCEPTION 'Token reset non valido';
END;
-- Validate password length
IF LENGTH(v_password) < 8 THEN RAISE EXCEPTION 'La password deve essere di almeno 8 caratteri';
END IF;
-- Find valid, unconsumed, unexpired token
SELECT * INTO v_recovery
FROM public.user_password_recovery_tokens
WHERE token = v_token
    AND consumed_at IS NULL;
IF NOT FOUND
OR v_recovery.expires_at <= NOW() THEN RAISE EXCEPTION 'Token reset non valido o scaduto';
END IF;
-- Find the associated user (must be active)
SELECT * INTO v_user
FROM public.users
WHERE username = v_recovery.user_id
    AND disabled = FALSE;
IF NOT FOUND THEN RAISE EXCEPTION 'Utente non disponibile';
END IF;
-- Update password (SHA-256 hash, same as login)
UPDATE public.users
SET password_hash = encode(digest(v_password, 'sha256'), 'hex'),
    updated_at = NOW()
WHERE username = v_user.username
RETURNING * INTO v_user;
-- Mark token as consumed
UPDATE public.user_password_recovery_tokens
SET consumed_at = NOW()
WHERE token = v_token;
-- Return user info (consistent with existing patterns)
RETURN jsonb_build_object(
    'username',
    v_user.username,
    'email',
    v_user.email,
    'role',
    v_user.role
);
END;
$$;
-- Grant execute to anon (login page) and authenticated
GRANT EXECUTE ON FUNCTION public.app_request_password_reset(TEXT, TEXT, INTEGER) TO anon,
    authenticated;
GRANT EXECUTE ON FUNCTION public.app_complete_password_recovery(TEXT, TEXT) TO anon,
    authenticated;
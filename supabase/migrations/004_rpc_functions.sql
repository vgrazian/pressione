-- Pressione: RPC functions for auth, email, password recovery, and test data
-- Migration 004 — All tables use fully qualified names (public.users, etc.)
-- 1. Update user email
CREATE OR REPLACE FUNCTION public.update_user_email(p_username TEXT, p_new_email TEXT) RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$ BEGIN
UPDATE public.users
SET email = p_new_email,
    updated_at = now()
WHERE username = p_username;
END;
$$;
-- 2. Create password recovery token (valid for 1 hour)
CREATE OR REPLACE FUNCTION public.create_recovery_token(p_username TEXT) RETURNS TEXT LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_token TEXT;
BEGIN v_token := encode(gen_random_bytes(16), 'hex');
INSERT INTO public.settings (username, key, value, updated_at)
VALUES (p_username, '_recovery_' || v_token, '{}', now());
RETURN v_token;
END;
$$;
-- 3. Reset password using recovery token
CREATE OR REPLACE FUNCTION public.reset_password_with_token(p_token TEXT, p_new_password TEXT) RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_username TEXT;
BEGIN
SELECT username INTO v_username
FROM public.settings
WHERE key = '_recovery_' || p_token
    AND updated_at > now() - interval '1 hour'
LIMIT 1;
IF v_username IS NULL THEN RAISE EXCEPTION 'Token non valido o scaduto';
END IF;
UPDATE public.users
SET password_hash = p_new_password,
    updated_at = now()
WHERE username = v_username;
DELETE FROM public.settings
WHERE key = '_recovery_' || p_token;
END;
$$;
-- 4. Admin reset password for any user
CREATE OR REPLACE FUNCTION public.admin_reset_password(
        p_admin_username TEXT,
        p_target_username TEXT,
        p_new_password TEXT
    ) RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$ BEGIN IF NOT EXISTS (
        SELECT 1
        FROM public.users
        WHERE username = p_admin_username
            AND role = 'admin'
    ) THEN RAISE EXCEPTION 'Accesso non autorizzato';
END IF;
UPDATE public.users
SET password_hash = p_new_password,
    updated_at = now()
WHERE username = p_target_username;
END;
$$;
-- 5. Generate test readings
CREATE OR REPLACE FUNCTION public.generate_test_data(p_username TEXT, p_count INTEGER DEFAULT 30) RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE i INTEGER;
v_sys INTEGER;
v_dia INTEGER;
v_hr INTEGER;
v_ts TIMESTAMPTZ;
BEGIN FOR i IN 1..p_count LOOP v_sys := 110 + floor(random() * 50)::INTEGER;
v_dia := 65 + floor(random() * 30)::INTEGER;
v_hr := 60 + floor(random() * 30)::INTEGER;
v_ts := now() - (random() * interval '30 days');
INSERT INTO public.readings (
        username,
        systolic,
        diastolic,
        heart_rate,
        timestamp,
        notes
    )
VALUES (
        p_username,
        v_sys,
        v_dia,
        v_hr,
        v_ts,
        'Dato test auto-generato'
    );
END LOOP;
END;
$$;
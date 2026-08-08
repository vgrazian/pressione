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
-- 5. Generate test readings with realistic BP + time distribution
CREATE OR REPLACE FUNCTION public.generate_test_data(p_username TEXT, p_count INTEGER DEFAULT 30) RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE i INTEGER;
v_sys INTEGER;
v_dia INTEGER;
v_hr INTEGER;
v_ts TIMESTAMPTZ;
v_roll FLOAT;
v_hour INTEGER;
BEGIN FOR i IN 1..p_count LOOP -- Weighted BP distribution (~40/20/15/10/3/7/5)
v_roll := random();
IF v_roll < 0.40 THEN v_sys := 90 + floor(random() * 30)::INTEGER;
-- 90-119
v_dia := 60 + floor(random() * 20)::INTEGER;
-- 60-79
ELSIF v_roll < 0.60 THEN v_sys := 120 + floor(random() * 10)::INTEGER;
-- 120-129
v_dia := 65 + floor(random() * 20)::INTEGER;
-- 65-84
ELSIF v_roll < 0.75 THEN v_sys := 130 + floor(random() * 10)::INTEGER;
-- 130-139
v_dia := 80 + floor(random() * 10)::INTEGER;
-- 80-89
ELSIF v_roll < 0.85 THEN v_sys := 140 + floor(random() * 40)::INTEGER;
-- 140-179
v_dia := 90 + floor(random() * 20)::INTEGER;
-- 90-109
ELSIF v_roll < 0.88 THEN v_sys := 180 + floor(random() * 41)::INTEGER;
-- 180-220
v_dia := 110 + floor(random() * 21)::INTEGER;
-- 110-130
ELSIF v_roll < 0.95 THEN v_sys := 70 + floor(random() * 30)::INTEGER;
-- 70-99
v_dia := 40 + floor(random() * 25)::INTEGER;
-- 40-64
ELSE v_sys := 80 + floor(random() * 121)::INTEGER;
-- 80-200
v_dia := 40 + floor(random() * 91)::INTEGER;
-- 40-130  (unclassified)
END IF;
v_hr := 60 + floor(random() * 30)::INTEGER;
-- Time-of-day distribution: morning 30%, afternoon 35%, evening 25%, night 10%
v_roll := random();
IF v_roll < 0.30 THEN v_hour := 6 + floor(random() * 6)::INTEGER;
-- 6-11
ELSIF v_roll < 0.65 THEN v_hour := 12 + floor(random() * 6)::INTEGER;
-- 12-17
ELSIF v_roll < 0.90 THEN v_hour := 18 + floor(random() * 5)::INTEGER;
-- 18-22
ELSE v_hour := floor(random() * 6)::INTEGER;
-- 0-5
END IF;
v_ts := date_trunc(
    'day',
    now() - (floor(random() * 30)::INTEGER || ' days')::INTERVAL
) + (v_hour || ' hours')::INTERVAL + (floor(random() * 60) || ' minutes')::INTERVAL;
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
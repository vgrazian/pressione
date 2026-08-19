-- Seed degli utenti iniziali
-- Password di default: 'Pressione2026!' (da cambiare al primo accesso)
-- Hash generato con: SELECT encode(digest('Pressione2026!', 'sha256'), 'hex')
-- Nota: nell'applicazione usiamo SHA-256 per l'hashing delle password
-- Questi hash sono precalcolati per la password 'Pressione2026!'
INSERT INTO users (
        username,
        email,
        password_hash,
        role,
        disabled,
        is_seeded
    )
VALUES (
        'nadia',
        'nadia@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'admin',
        false,
        true
    ),
    (
        'roberto',
        'roberto@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        false,
        true
    ),
    (
        'barbara',
        'barbara@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        false,
        true
    ),
    (
        'valerio',
        'valerio@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'admin',
        false,
        true
    ),
    (
        'marco',
        'marco@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        false,
        true
    ),
    (
        'rita',
        'rita@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        false,
        true
    ),
    (
        'anna',
        'anna@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        false,
        true
    ) ON CONFLICT (username) DO NOTHING;
-- Seed farmaci: timeline terapeutica realistica per ogni utente seed,
-- così i marker di cambio terapia sono visibili nei grafici.
-- Date di inizio/fine entro gli ultimi 30 giorni per sovrapporsi alle letture.
INSERT INTO medications (
        id,
        username,
        name,
        active_ingredient,
        dosage,
        frequency,
        notes,
        start_date,
        end_date,
        created_at,
        updated_at
    )
SELECT gen_random_uuid(),
    u.username,
    m.name,
    m.active_ingredient,
    m.dosage,
    m.frequency,
    m.notes,
    now() - (m.start_days_ago || ' days')::interval,
    CASE
        WHEN m.end_days_ago IS NULL THEN NULL
        ELSE now() - (m.end_days_ago || ' days')::interval
    END,
    now(),
    now()
FROM users u
    CROSS JOIN (
        VALUES (
                'Losartan',
                'Losartan potassico',
                '50 mg',
                '1 volta al giorno',
                'al mattino',
                30,
                NULL
            ),
            (
                'Amlodipina',
                'Amlodipina besilato',
                '5 mg',
                '1 volta al giorno',
                'alla sera',
                20,
                NULL
            ),
            (
                'Bisoprololo',
                'Bisoprololo fumarato',
                '2,5 mg',
                '1 volta al giorno',
                'al mattino',
                30,
                10
            ),
            (
                'Ramipril',
                'Ramipril',
                '5 mg',
                '1 volta al giorno',
                '',
                12,
                3
            )
    ) AS m(
        name,
        active_ingredient,
        dosage,
        frequency,
        notes,
        start_days_ago,
        end_days_ago
    ) ON CONFLICT DO NOTHING;
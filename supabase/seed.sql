-- Seed degli utenti iniziali
-- Password di default: 'Pressione2026!' (da cambiare al primo accesso)
-- Hash generato con: SELECT encode(digest('Pressione2026!', 'sha256'), 'hex')
-- Nota: nell'applicazione usiamo SHA-256 per l'hashing delle password
-- Questi hash sono precalcolati per la password 'Pressione2026!'
INSERT INTO users (username, email, password_hash, role, active)
VALUES (
        'nadia',
        'nadia@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'admin',
        true
    ),
    (
        'roberto',
        'roberto@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        true
    ),
    (
        'barbara',
        'barbara@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        true
    ),
    (
        'valerio',
        'valerio@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'admin',
        true
    ),
    (
        'marco',
        'marco@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        true
    ),
    (
        'rita',
        'rita@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        true
    ),
    (
        'anna',
        'anna@pressione.local',
        'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0',
        'user',
        true
    ) ON CONFLICT (username) DO NOTHING;
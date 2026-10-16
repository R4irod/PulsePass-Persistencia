INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('VEN-SMR-01', 'Marina Convention Center', 'Santa Marta', 'Av. del Mar', 3000, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
SELECT 'CMF-2026', 'Caribbean Music Fest', 'Festival de música del Caribe', 'MUSIC', 'DRAFT',
       NOW() + INTERVAL '60 days', 18, v.id
FROM venues v
WHERE v.code = 'VEN-SMR-01'
ON CONFLICT (event_code) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'STAFF' FROM users WHERE username = 'laura'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'miguel'
ON CONFLICT DO NOTHING;

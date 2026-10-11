-- Seed data for local use and review. Plain SQL that runs on PostgreSQL and H2.
-- Passwords are BCrypt hashes. The plain text demo passwords are documented in README.md.
-- Change them before any real deployment.

INSERT INTO users (id, username, password_hash, role, created_at, updated_at, created_by, updated_by)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'admin', '$2y$10$csXNoVMAARn8pSq5Zerwu.8gAJ8ROhSSagN5sHzv/RnQht4E9nb12', 'ADMIN',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('a0000000-0000-0000-0000-000000000002', 'ops', '$2y$10$iG2Vvl2rgzK8OoUGr0gmnea.efl0OBW2zQ.2z1m6E8Eqmm1aDm4SK', 'OPERATIONS',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system');

-- Sample trek from the assignment: starts 2026-08-10 06:00 IST, last checkpoint 84 hours later.
INSERT INTO treks (id, name, start_at, end_at, status, created_at, updated_at, created_by, updated_by)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Kedarkantha Trek',
     TIMESTAMP WITH TIME ZONE '2026-08-10 06:00:00+05:30',
     TIMESTAMP WITH TIME ZONE '2026-08-13 18:00:00+05:30',
     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system');

-- Checkpoints and expected hours after the trek start, as listed in the assignment.
INSERT INTO checkpoints (id, trek_id, name, sequence_no, expected_after_hours,
                         created_at, updated_at, created_by, updated_by)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
     'Dehradun', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001',
     'Sankri', 2, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001',
     'Juda Ka Talab', 3, 28, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001',
     'Kedarkantha Summit', 4, 48, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('c0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001',
     'Sankri', 5, 72, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('c0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000001',
     'Dehradun', 6, 84, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system');

-- Sample participants with made up contact numbers. Two are on the trek, one has not started.
INSERT INTO participants (id, trek_id, name, phone, emergency_contact, status,
                          created_at, updated_at, created_by, updated_by)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001',
     'Aarav Sharma', '+91 90000 00001', '+91 90000 10001', 'ON_TREK',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001',
     'Diya Mehta', '+91 90000 00002', '+91 90000 10002', 'ON_TREK',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system'),
    ('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001',
     'Rohan Verma', '+91 90000 00003', '+91 90000 10003', 'REGISTERED',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system');

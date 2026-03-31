INSERT INTO users (username, password, role)
SELECT 'admin', 'admin123', 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

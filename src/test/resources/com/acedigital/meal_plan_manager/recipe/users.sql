DELETE FROM users;
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;

-- Password column stores a bcrypt hash (not a plaintext password).
-- This hash was generated for the string 'password-of-length-12' and is
-- here only because the H2 test fixture bypasses UserService.createUser.
-- The bcrypt cost is 10; regenerate with:
--   new BCryptPasswordEncoder().encode("password-of-length-12")
INSERT INTO users(username, email, password)
VALUES ('somebody', 'somebody@mail.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

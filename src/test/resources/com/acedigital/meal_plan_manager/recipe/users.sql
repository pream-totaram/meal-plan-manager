DELETE FROM users;
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;
INSERT INTO users(username, email, password)
VALUES ('somebody', 'somebody@mail.com', 'password');


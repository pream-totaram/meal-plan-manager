-- -- Clear any existing data
-- DELETE FROM recipes;
-- DELETE FROM users;
--
-- -- Reset auto-increment to ensure we get ID 1
-- ALTER TABLE recipes ALTER COLUMN id RESTART WITH 1;
-- ALTER TABLE users ALTER COLUMN id RESTART WITH 1;

INSERT INTO users(username, email, password)
VALUES ('somebody', 'somebody@mail.com', 'password');

INSERT INTO recipes(title, description, prep_time, cook_time, user_id) VALUES 
('Spaghetti Carbonara', 'Classic Italian pasta dish', 15, 20, 1),
('Chicken Curry', 'Spicy Indian curry', 20, 30, 1),
('Caesar Salad', 'Fresh romaine lettuce with Caesar dressing', 10, 0, 1);

-- Add a deleted record to test soft delete functionality
INSERT INTO recipes (title, description, prep_time, cook_time, deleted, deleted_at, user_id) VALUES 
('Deleted Recipe', 'This recipe should not appear in results', 5, 10, true, CURRENT_TIMESTAMP, 1);

-- -- Add a specific recipe with ID 100 for the delete test
-- INSERT INTO recipes(id, title, description, prep_time, cook_time, user_id) 
-- VALUES (100, 'Recipe to delete', 'It dont matter', 15, 20, 1);

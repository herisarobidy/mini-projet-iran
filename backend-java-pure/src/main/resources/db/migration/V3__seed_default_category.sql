INSERT INTO categories (name, slug, description)
SELECT 'General', 'general', 'Catégorie par défaut'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'general');

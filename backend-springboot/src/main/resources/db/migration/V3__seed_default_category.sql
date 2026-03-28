INSERT INTO categories (name, slug, description)
VALUES ('Actualités', 'actualites', 'Catégorie par défaut')
ON CONFLICT (slug) DO NOTHING;

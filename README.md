# iran-war-platform

Mono-repo:
- backend-java-pure
- mobile-vue
- docker
- documentation

## Description

Plateforme d'articles (front-office) avec un espace d'administration (CRUD articles & catégories) et un système de commentaires avec modération.

## Prérequis

- Java 17+
- Maven 3.9+
- PostgreSQL 16 (si exécution en local)
- Docker + Docker Compose (si exécution en conteneurs)

## Démarrage en local (backend)

1. Démarrer PostgreSQL et créer la base:

   - DB: `iran_war_db`
   - User: `iran`
   - Password: `iran`

2. Lancer le backend Java (sans Spring):

   - Ouvrir le dossier `backend-java-pure`
   - Builder et lancer:

     - `mvn -q -DskipTests clean package`
     - `java -jar target/backend-java-pure-0.0.1-SNAPSHOT-all.jar`

3. Accès:

   - Front-office: http://localhost:8080/

## Démarrage avec Docker (backend + PostgreSQL)

Le `docker-compose.yml` est dans `docker/`.

1. Depuis le dossier `docker/`:

   - `docker compose up --build`

2. Accès:

   - Front-office: http://localhost:8080/

## Variables d'environnement (backend)

- `SERVER_PORT` (défaut: `8080`)
- `SPRING_DATASOURCE_URL` (défaut: `jdbc:postgresql://localhost:5432/iran_war_db`)
- `SPRING_DATASOURCE_USERNAME` (défaut: `iran`)
- `SPRING_DATASOURCE_PASSWORD` (défaut: `iran`)
- `PUBLIC_BASE_URL` (défaut: `http://localhost:8080`) utilisé pour générer les URLs dans `/sitemap.xml`
- `FLYWAY_REPAIR` (défaut: `false`) si `true`, exécute `flyway.repair()` au démarrage (utile quand un volume Docker contient un historique Flyway avec checksum mismatch)

## URLs utiles

- Front-office:

  - `/` (liste paginée)
  - `/articles/{slug}` (détail + commentaires)

- Admin:

  - `/admin/login`
  - `/admin` (dashboard)
  - `/admin/articles`
  - `/admin/categories`
  - `/admin/comments`

- SEO/tech:

  - `/robots.txt`
  - `/sitemap.xml`

## SEO

- La zone `/admin/` est volontairement exclue de l'indexation via `robots.txt`.
- Les tests Lighthouse/SEO doivent être réalisés sur le front-office (ex: `/` et `/articles/{slug}`).

## Dépannage

- Si la base locale ne contient pas les mêmes données que Docker: ce sont 2 bases différentes (PostgreSQL local vs conteneur).
- En Docker, le backend écoute sur le port `8080` (variable `SERVER_PORT=8080` dans `docker/docker-compose.yml`).

## Identifiants admin

- Username: `admin`
- Password: `admin123`

## Schéma de base de données

Tables:

- `users`
- `categories`
- `articles`
- `comments`

Relations:

- `articles.category_id` -> `categories.id` (FK, `ON DELETE SET NULL`)
- `comments.article_id` -> `articles.id` (FK, `ON DELETE CASCADE`)


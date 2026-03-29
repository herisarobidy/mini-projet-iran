# iran-war-platform

Mono-repo:
- backend-springboot
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

2. Lancer Spring Boot:

   - Ouvrir le dossier `backend-springboot`
   - Démarrer l'application (IDE) ou:

     - `mvn spring-boot:run`

3. Accès:

   - Front-office: http://localhost:8383/

## Démarrage avec Docker (backend + PostgreSQL)

Le `docker-compose.yml` est dans `docker/`.

1. Depuis le dossier `docker/`:

   - `docker compose up --build`

2. Accès:

   - Front-office: http://localhost:8080/

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

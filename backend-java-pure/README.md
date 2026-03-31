# backend-java-pure

Backend Java sans framework web (serveur JDK `HttpServer`), avec Thymeleaf standalone + JDBC (HikariCP) + Flyway.

## Démarrage

Variables d'environnement supportées:

- `SERVER_PORT` (défaut: `8080`)
- `SPRING_DATASOURCE_URL` (défaut: `jdbc:postgresql://localhost:5432/iran_war_db`)
- `SPRING_DATASOURCE_USERNAME` (défaut: `iran`)
- `SPRING_DATASOURCE_PASSWORD` (défaut: `iran`)
- `PUBLIC_BASE_URL` (défaut: `http://localhost:8080`)
- `FLYWAY_REPAIR` (défaut: `false`)

### Lancer

- `mvn -q -DskipTests clean package`
- `java -jar target/backend-java-pure-0.0.1-SNAPSHOT-all.jar`

## Admin

- Username: `admin`
- Password: `admin123`

## Docker

Le `docker-compose.yml` est dans `../docker/`.

- `docker compose up --build`

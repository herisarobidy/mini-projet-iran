package com.project.pure;

import com.project.pure.dao.ArticleDao;
import com.project.pure.dao.CategoryDao;
import com.project.pure.dao.CommentDao;
import com.project.pure.dao.UserDao;
import com.project.pure.admin.ArticleForm;
import com.project.pure.admin.CategoryForm;
import com.project.pure.http.Router;
import com.project.pure.http.QueryParams;
import com.project.pure.http.FormData;
import com.project.pure.http.Responses;
import com.project.pure.http.SessionManager;
import com.project.pure.http.StaticFileHandler;
import com.project.pure.security.Csrf;
import com.project.pure.template.TemplateRenderer;
import com.project.pure.util.Env;
import com.project.pure.db.Database;
import com.project.pure.db.FlywayMigrator;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static Map<String, Object> csrfModel(Csrf.Token token) {
        return Map.of(
                "parameterName", token.parameterName(),
                "token", token.token()
        );
    }

    private static SessionManager.Session requireAdmin(SessionManager sessions, com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
        var session = sessions.get(exchange);
        if (session == null || !ROLE_ADMIN.equals(session.data().get("role"))) {
            Responses.redirect(exchange, "/admin/login");
            return null;
        }
        return session;
    }

    private static boolean requireCsrf(SessionManager.Session session, java.util.Map<String, String> form) {
        if (session == null) {
            return false;
        }
        String token = form.get("_csrf");
        boolean ok = Csrf.validate(session, token);
        if (!ok) {
            Object expected = session.data().get("csrf");
            System.out.println("[CSRF] invalid token received='" + token + "' expected='" + expected + "'");
        }
        return ok;
    }

    private static boolean parseBooleanField(java.util.Map<String, String> form, String name) {
        String v = form.get(name);
        if (v == null) {
            return false;
        }
        String s = v.trim().toLowerCase();
        return s.equals("true") || s.equals("on") || s.equals("1") || s.equals("yes");
    }

    public static void main(String[] args) throws Exception {
        int port = Env.getInt("SERVER_PORT", 8080);

        Database db = Database.fromEnv();
        FlywayMigrator.migrate(db);

        TemplateRenderer renderer = TemplateRenderer.fromClasspath();
        ArticleDao articleDao = new ArticleDao(db);
        CategoryDao categoryDao = new CategoryDao(db);
        CommentDao commentDao = new CommentDao(db);
        UserDao userDao = new UserDao(db);
        SessionManager sessions = new SessionManager();

        Router router = new Router();

        StaticFileHandler staticHandler = new StaticFileHandler();
        router.get("/css/.*", staticHandler::handle);
        router.get("/js/.*", staticHandler::handle);
        router.get("/images/.*", staticHandler::handle);
        router.get("/vendor/.*", staticHandler::handle);

        router.get("/robots.txt", exchange -> {
            String baseUrl = Env.get("PUBLIC_BASE_URL", "http://localhost:" + port);
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            String bodyStr = "User-agent: *\n"
                    + "Disallow: /admin/\n\n"
                    + "Sitemap: " + baseUrl + "/sitemap.xml\n";
            byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        });

        router.get("/sitemap.xml", exchange -> {
            String baseUrl = Env.get("PUBLIC_BASE_URL", "http://localhost:" + port);
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            var urls = articleDao.listPublishedForSitemap(50000);

            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

            sb.append("  <url>\n");
            sb.append("    <loc>").append(baseUrl).append("/").append("</loc>\n");
            sb.append("  </url>\n");

            DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            for (var u : urls) {
                sb.append("  <url>\n");
                sb.append("    <loc>").append(baseUrl).append("/articles/").append(u.slug()).append("</loc>\n");
                OffsetDateTime lastMod = u.lastModified();
                if (lastMod != null) {
                    sb.append("    <lastmod>").append(fmt.format(lastMod)).append("</lastmod>\n");
                }
                sb.append("  </url>\n");
            }

            sb.append("</urlset>\n");

            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        });

        router.get("/", exchange -> {
            var query = QueryParams.parse(exchange.getRequestURI().getRawQuery());
            int page = Math.max(0, QueryParams.getInt(query, "page", 0));
            int size = 5;

            long total = articleDao.countPublished();
            int totalPages = (int) ((total + size - 1) / size);
            int safeTotalPages = Math.max(0, totalPages);
            int safePage = Math.min(page, Math.max(0, safeTotalPages - 1));

            var articles = articleDao.listPublishedPage(safePage, size);

            boolean hasPrev = safePage > 0;
            boolean hasNext = (safePage + 1) < safeTotalPages;

            Map<String, Object> model = Map.of(
                    "articles", articles,
                    "currentPage", safePage,
                    "totalPages", safeTotalPages,
                    "hasPrev", hasPrev,
                    "hasNext", hasNext
            );
            renderer.render(exchange, "index", model);
        });

        router.get("/articles/[^/]+", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String slug = path.substring("/articles/".length());

            var article = articleDao.findBySlug(slug);
            if (article == null || !article.published()) {
                Responses.notFound(exchange);
                return;
            }

            var comments = commentDao.listApprovedForArticle(article.id());
            Map<String, Object> model = Map.of(
                    "article", article,
                    "comments", comments
            );
            renderer.render(exchange, "articles/detail", model);
        });

        router.post("/articles/[^/]+/comments", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String prefix = "/articles/";
            String suffix = "/comments";
            String slug = path.substring(prefix.length(), path.length() - suffix.length());

            var article = articleDao.findBySlug(slug);
            if (article == null || !article.published()) {
                Responses.notFound(exchange);
                return;
            }

            var form = FormData.parseUrlEncoded(exchange);
            String author = form.get("author");
            String content = form.get("content");

            if (author == null || author.isBlank() || content == null || content.isBlank()) {
                Responses.redirect(exchange, "/articles/" + slug + "?comment=error");
                return;
            }

            commentDao.create(article.id(), author.trim(), content.trim());
            Responses.redirect(exchange, "/articles/" + slug + "?comment=sent");
        });

        router.get("/admin/login", exchange -> {
            var session = sessions.getOrCreate(exchange);
            var csrf = Csrf.ensure(session);
            Map<String, Object> model = Map.of(
                    "_csrf", csrfModel(csrf)
            );
            renderer.render(exchange, "admin/login", model);
        });

        router.post("/admin/login", exchange -> {
            var session = sessions.getOrCreate(exchange);
            var form = FormData.parseUrlEncoded(exchange);

            String csrfToken = form.get("_csrf");
            if (!Csrf.validate(session, csrfToken)) {
                Responses.redirect(exchange, "/admin/login?error=true");
                return;
            }

            String username = form.get("username");
            String password = form.get("password");
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                Responses.redirect(exchange, "/admin/login?error=true");
                return;
            }

            var user = userDao.findByUsername(username.trim());
            if (user == null || !password.equals(user.password()) || !ROLE_ADMIN.equals(user.role())) {
                Responses.redirect(exchange, "/admin/login?error=true");
                return;
            }

            session.data().put("username", user.username());
            session.data().put("role", user.role());
            Csrf.ensure(session);
            Responses.redirect(exchange, "/admin");
        });

        router.post("/admin/logout", exchange -> {
            var session = sessions.get(exchange);
            if (session == null) {
                Responses.redirect(exchange, "/");
                return;
            }

            var form = FormData.parseUrlEncoded(exchange);
            String csrfToken = form.get("_csrf");
            if (!Csrf.validate(session, csrfToken)) {
                Responses.redirect(exchange, "/admin");
                return;
            }

            sessions.invalidate(exchange);
            Responses.redirect(exchange, "/");
        });

        router.get("/admin", exchange -> {
            var session = sessions.get(exchange);
            if (session == null || !ROLE_ADMIN.equals(session.data().get("role"))) {
                Responses.redirect(exchange, "/admin/login");
                return;
            }

            var csrf = Csrf.ensure(session);
            Map<String, Object> model = Map.of(
                    "_csrf", csrfModel(csrf)
            );
            renderer.render(exchange, "admin/dashboard", model);
        });

        router.get("/admin/articles", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            var csrf = Csrf.ensure(session);
            Map<String, Object> model = Map.of(
                    "articles", articleDao.listAll(),
                    "_csrf", csrfModel(csrf)
            );
            renderer.render(exchange, "admin/articles/list", model);
        });

        router.get("/admin/articles/new", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            var csrf = Csrf.ensure(session);
            Map<String, Object> model = Map.of(
                    "form", new ArticleForm(),
                    "categories", categoryDao.listAll(),
                    "_csrf", csrfModel(csrf)
            );
            renderer.render(exchange, "admin/articles/form", model);
        });

        router.post("/admin/articles", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            var form = FormData.parseUrlEncoded(exchange);
            System.out.println("[ADMIN] POST /admin/articles form=" + form);
            if (!requireCsrf(session, form)) {
                Responses.forbidden(exchange, "CSRF invalid");
                return;
            }

            String title = form.get("title");
            String content = form.get("content");
            String author = form.get("author");
            String image = form.get("image");
            String categoryId = form.get("categoryId");
            boolean published = parseBooleanField(form, "published");

            ArticleForm f = new ArticleForm();
            f.setTitle(title);
            f.setContent(content);
            f.setAuthor(author);
            f.setImage(image);
            if (categoryId != null && !categoryId.isBlank()) {
                try {
                    f.setCategoryId(Long.parseLong(categoryId));
                } catch (NumberFormatException ignored) {
                }
            }
            f.setPublished(published);

            if (title == null || title.isBlank() || content == null || content.isBlank() || author == null || author.isBlank() || f.getCategoryId() == null) {
                var csrf = Csrf.ensure(session);
                Map<String, Object> model = Map.of(
                        "form", f,
                        "categories", categoryDao.listAll(),
                        "_csrf", csrfModel(csrf)
                );
                renderer.render(exchange, "admin/articles/form", model);
                return;
            }

            long id = articleDao.create(title, content, author, f.getCategoryId(), published, image);
            System.out.println("[ADMIN] Created article id=" + id + " title='" + title + "' categoryId=" + f.getCategoryId());
            Responses.redirect(exchange, "/admin/articles");
        });

        router.get("/admin/articles/\\d+/edit", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/admin/articles/".length(), path.length() - "/edit".length());
            long id = Long.parseLong(idStr);
            var a = articleDao.findById(id);
            if (a == null) {
                Responses.redirect(exchange, "/admin/articles");
                return;
            }

            ArticleForm f = new ArticleForm();
            f.setTitle(a.title());
            f.setContent(a.content());
            f.setAuthor(a.author());
            f.setImage(a.image());
            f.setPublished(a.published());
            f.setCategoryId(a.categoryId());

            var csrf = Csrf.ensure(session);
            Map<String, Object> model = Map.of(
                    "form", f,
                    "articleId", id,
                    "categories", categoryDao.listAll(),
                    "_csrf", csrfModel(csrf)
            );
            renderer.render(exchange, "admin/articles/form", model);
        });

        router.post("/admin/articles/\\d+", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/admin/articles/".length());
            long id = Long.parseLong(idStr);

            var form = FormData.parseUrlEncoded(exchange);
            if (!requireCsrf(session, form)) {
                Responses.redirect(exchange, "/admin/articles");
                return;
            }

            String title = form.get("title");
            String content = form.get("content");
            String author = form.get("author");
            String image = form.get("image");
            String categoryId = form.get("categoryId");
            boolean published = parseBooleanField(form, "published");

            ArticleForm f = new ArticleForm();
            f.setTitle(title);
            f.setContent(content);
            f.setAuthor(author);
            f.setImage(image);
            if (categoryId != null && !categoryId.isBlank()) {
                try {
                    f.setCategoryId(Long.parseLong(categoryId));
                } catch (NumberFormatException ignored) {
                }
            }
            f.setPublished(published);

            if (title == null || title.isBlank() || content == null || content.isBlank() || author == null || author.isBlank() || f.getCategoryId() == null) {
                var csrf = Csrf.ensure(session);
                Map<String, Object> model = Map.of(
                        "form", f,
                        "articleId", id,
                        "categories", categoryDao.listAll(),
                        "_csrf", csrfModel(csrf)
                );
                renderer.render(exchange, "admin/articles/form", model);
                return;
            }

            articleDao.update(id, title, content, author, f.getCategoryId(), published, image);
            System.out.println("[ADMIN] Updated article id=" + id + " title='" + title + "' categoryId=" + f.getCategoryId());
            Responses.redirect(exchange, "/admin/articles");
        });

        router.post("/admin/articles/\\d+/delete", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/admin/articles/".length(), path.length() - "/delete".length());
            long id = Long.parseLong(idStr);

            var form = FormData.parseUrlEncoded(exchange);
            if (!requireCsrf(session, form)) {
                Responses.forbidden(exchange, "CSRF invalid");
                return;
            }

            articleDao.delete(id);
            Responses.redirect(exchange, "/admin/articles");
        });

        router.get("/admin/categories", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            var query = QueryParams.parse(exchange.getRequestURI().getRawQuery());
            String error = query.get("error");

            var csrf = Csrf.ensure(session);
            Map<String, Object> model = new HashMap<>();
            model.put("categories", categoryDao.listAll());
            model.put("error", error);
            model.put("_csrf", csrfModel(csrf));
            renderer.render(exchange, "admin/categories/list", model);
        });

        router.get("/admin/categories/new", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            var csrf = Csrf.ensure(session);
            Map<String, Object> model = Map.of(
                    "form", new CategoryForm(),
                    "_csrf", csrfModel(csrf)
            );
            renderer.render(exchange, "admin/categories/form", model);
        });

        router.post("/admin/categories", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            var form = FormData.parseUrlEncoded(exchange);
            System.out.println("[ADMIN] POST /admin/categories form=" + form);
            if (!requireCsrf(session, form)) {
                Responses.forbidden(exchange, "CSRF invalid");
                return;
            }

            String name = form.get("name");
            String description = form.get("description");

            CategoryForm f = new CategoryForm();
            f.setName(name);
            f.setDescription(description);

            if (name == null || name.isBlank()) {
                var csrf = Csrf.ensure(session);
                Map<String, Object> model = Map.of(
                        "form", f,
                        "_csrf", csrfModel(csrf)
                );
                renderer.render(exchange, "admin/categories/form", model);
                return;
            }

            var created = categoryDao.create(name, description);
            System.out.println("[ADMIN] Created category id=" + created.id() + " name='" + name + "'");
            Responses.redirect(exchange, "/admin/categories");
        });

        router.get("/admin/categories/\\d+/edit", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/admin/categories/".length(), path.length() - "/edit".length());
            long id = Long.parseLong(idStr);
            var c = categoryDao.findById(id);
            if (c == null) {
                Responses.redirect(exchange, "/admin/categories");
                return;
            }

            CategoryForm f = new CategoryForm();
            f.setName(c.name());
            f.setDescription(c.description());

            var csrf = Csrf.ensure(session);
            Map<String, Object> model = Map.of(
                    "form", f,
                    "categoryId", id,
                    "_csrf", csrfModel(csrf)
            );
            renderer.render(exchange, "admin/categories/form", model);
        });

        router.post("/admin/categories/\\d+", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/admin/categories/".length());
            long id = Long.parseLong(idStr);

            var form = FormData.parseUrlEncoded(exchange);
            if (!requireCsrf(session, form)) {
                Responses.forbidden(exchange, "CSRF invalid");
                return;
            }

            String name = form.get("name");
            String description = form.get("description");

            CategoryForm f = new CategoryForm();
            f.setName(name);
            f.setDescription(description);

            if (name == null || name.isBlank()) {
                var csrf = Csrf.ensure(session);
                Map<String, Object> model = Map.of(
                        "form", f,
                        "categoryId", id,
                        "_csrf", csrfModel(csrf)
                );
                renderer.render(exchange, "admin/categories/form", model);
                return;
            }

            categoryDao.update(id, name, description);
            System.out.println("[ADMIN] Updated category id=" + id + " name='" + name + "'");
            Responses.redirect(exchange, "/admin/categories");
        });

        router.post("/admin/categories/\\d+/delete", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/admin/categories/".length(), path.length() - "/delete".length());
            long id = Long.parseLong(idStr);

            var form = FormData.parseUrlEncoded(exchange);
            if (!requireCsrf(session, form)) {
                Responses.forbidden(exchange, "CSRF invalid");
                return;
            }

            if (categoryDao.countArticlesUsing(id) > 0) {
                Responses.redirect(exchange, "/admin/categories?error=in_use");
                return;
            }

            categoryDao.delete(id);
            Responses.redirect(exchange, "/admin/categories");
        });

        router.get("/admin/comments", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            var csrf = Csrf.ensure(session);
            Map<String, Object> model = Map.of(
                    "pendingComments", commentDao.listPending(),
                    "approvedComments", commentDao.listApproved(),
                    "_csrf", csrfModel(csrf)
            );
            renderer.render(exchange, "admin/comments/list", model);
        });

        router.post("/admin/comments/\\d+/approve", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/admin/comments/".length(), path.length() - "/approve".length());
            long id = Long.parseLong(idStr);

            var form = FormData.parseUrlEncoded(exchange);
            if (!requireCsrf(session, form)) {
                Responses.forbidden(exchange, "CSRF invalid");
                return;
            }

            commentDao.approve(id);
            Responses.redirect(exchange, "/admin/comments");
        });

        router.post("/admin/comments/\\d+/delete", exchange -> {
            var session = requireAdmin(sessions, exchange);
            if (session == null) return;

            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring("/admin/comments/".length(), path.length() - "/delete".length());
            long id = Long.parseLong(idStr);

            var form = FormData.parseUrlEncoded(exchange);
            if (!requireCsrf(session, form)) {
                Responses.forbidden(exchange, "CSRF invalid");
                return;
            }

            commentDao.delete(id);
            Responses.redirect(exchange, "/admin/comments");
        });

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", router);
        server.setExecutor(null);

        System.out.println("backend-java-pure listening on http://localhost:" + port + "/");
        server.start();
    }
}

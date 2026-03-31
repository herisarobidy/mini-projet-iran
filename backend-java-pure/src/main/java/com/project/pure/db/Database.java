package com.project.pure.db;

import com.project.pure.util.Env;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class Database {

    private final DataSource dataSource;

    private Database(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static Database fromEnv() {
        String url = Env.get("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/iran_war_db");
        String username = Env.get("SPRING_DATASOURCE_USERNAME", "iran");
        String password = Env.get("SPRING_DATASOURCE_PASSWORD", "iran");

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setInitializationFailTimeout(-1);
        cfg.setMaximumPoolSize(10);
        cfg.setPoolName("iran-hikari");

        return new Database(new HikariDataSource(cfg));
    }

    public DataSource dataSource() {
        return dataSource;
    }
}

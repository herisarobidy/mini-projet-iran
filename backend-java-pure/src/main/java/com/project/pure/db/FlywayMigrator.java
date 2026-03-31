package com.project.pure.db;

import com.project.pure.util.Env;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;

public final class FlywayMigrator {

    private FlywayMigrator() {}

    public static void migrate(Database db) {
        Flyway flyway = Flyway.configure()
                .dataSource(db.dataSource())
                .locations("classpath:db/migration")
                .load();

        boolean repair = "true".equalsIgnoreCase(Env.get("FLYWAY_REPAIR", "false"));
        if (repair) {
            flyway.repair();
        }

        try {
            flyway.migrate();
        } catch (FlywayValidateException ex) {
            if (repair) {
                flyway.repair();
                flyway.migrate();
                return;
            }
            throw ex;
        }
    }
}

package com.example.jpaspringboot.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(named = "hnblue.mysql.migration.it", matches = "true")
class RealMySqlMigrationIntegrationTest {
    @Test
    void appliesVersionedGovernanceMigrationsToRealMySql() throws Exception {
        String url = required("spring.datasource.url");
        String username = required("spring.datasource.username");
        String password = System.getProperty("spring.datasource.password", "");
        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();
        assertEquals(2, alreadyApplied(flyway));

        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            assertEquals(1, scalar(statement, """
                    SELECT COUNT(*) FROM information_schema.tables
                     WHERE table_schema = DATABASE() AND table_name = 'agent_dispatch_outbox'
                    """));
            assertEquals(0, scalar(statement, """
                    SELECT COUNT(*) FROM information_schema.tables
                     WHERE table_schema = DATABASE() AND table_name = 'agent_task_outbox'
                    """));
            assertEquals(4, scalar(statement, """
                    SELECT COUNT(*) FROM information_schema.columns
                     WHERE table_schema = DATABASE() AND table_name = 'agent_task'
                       AND column_name IN ('owner_username', 'owner_token_version',
                                           'password_change_required', 'input_message')
                    """));
            assertEquals(2, scalar(statement,
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1"));
        }
    }

    private int alreadyApplied(Flyway flyway) {
        return (int) java.util.Arrays.stream(flyway.info().applied()).count();
    }

    private int scalar(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private String required(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException(key + " is required");
        return value;
    }
}

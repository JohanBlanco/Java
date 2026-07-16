package com.gymplatform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Parches idempotentes para H2 cuando ddl-auto no añade columnas nuevas en tablas existentes.
 */
@Component
public class DatabaseSchemaPatch implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPatch.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaPatch(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing(
                "broadcast_channel_settings",
                "whatsapp_web_session_confirmed",
                "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing("users", "national_id", "VARCHAR(16)");
        addColumnIfMissing("products", "apply_iva", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing("products", "iva_percent", "DECIMAL(7,2)");
        addColumnIfMissing("membership_packages", "apply_iva", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing("membership_packages", "iva_percent", "DECIMAL(7,2)");
        addColumnIfMissing(
                "cash_register_configs",
                "system_iva_percent",
                "DECIMAL(7,2) NOT NULL DEFAULT 13");
        addColumnIfMissing("store_sales", "payment_method", "VARCHAR(16)");
        addColumnIfMissing("store_sales", "payment_proof_data", "CLOB");
        backfillUserNationalIds();
    }

    private void backfillUserNationalIds() {
        jdbcTemplate.update("""
                UPDATE users u
                SET national_id = (
                    SELECT REGEXP_REPLACE(COALESCE(mp.national_id, ''), '[^0-9]', '')
                    FROM member_profiles mp
                    WHERE mp.user_id = u.id
                )
                WHERE (u.national_id IS NULL OR TRIM(u.national_id) = '')
                  AND EXISTS (
                    SELECT 1 FROM member_profiles mp
                    WHERE mp.user_id = u.id
                      AND mp.national_id IS NOT NULL
                      AND TRIM(mp.national_id) <> ''
                  )
                """);
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """,
                Integer.class,
                table.toUpperCase(),
                column.toUpperCase());
        if (count != null && count > 0) {
            return;
        }
        log.info("Aplicando parche de esquema: {}.{}", table, column);
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }
}

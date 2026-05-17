package com.quickbite.order.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * One-time schema fix: widens the order_status and mode_of_payment columns
 * so they can hold all enum values (e.g. OUT_FOR_DELIVERY = 16 chars).
 *
 * Hibernate ddl-auto=update does NOT widen existing columns, so we do it
 * manually on startup. The ALTER statements are idempotent — safe to run
 * every time.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaFixRunner implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE orders MODIFY COLUMN order_status VARCHAR(30) NOT NULL");
            log.info("✅ Schema fix applied: order_status column widened to VARCHAR(30)");
        } catch (Exception e) {
            // Table might not exist yet (first run) or column already correct
            log.debug("Schema fix skipped (probably already applied): {}", e.getMessage());
        }
    }
}

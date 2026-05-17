-- Fix order_status column: Hibernate ddl-auto=update does not widen existing columns.
-- This runs on EVERY startup (idempotent) to ensure the column can hold all enum values.
ALTER TABLE orders MODIFY COLUMN order_status VARCHAR(30) NOT NULL;

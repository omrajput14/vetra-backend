-- V34: Make legacy V2 columns nullable on notifications table
-- Enables persistence of modern Notification entity (V14+) without constraint violations

ALTER TABLE notifications ALTER COLUMN receiver_id DROP NOT NULL;
ALTER TABLE notifications ALTER COLUMN type DROP NOT NULL;

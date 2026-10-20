ALTER TABLE users ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE categories ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE activities ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE sessions ADD COLUMN created_by BIGINT NULL, ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE packages ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE file_objects ADD COLUMN unreferenced_at DATETIME(3) NULL, ADD COLUMN delete_requested_at DATETIME(3) NULL;
UPDATE file_objects f SET unreferenced_at=UTC_TIMESTAMP(3) WHERE NOT EXISTS(SELECT 1 FROM activity_images i WHERE i.file_object_id=f.id);
CREATE INDEX idx_files_cleanup ON file_objects(status,unreferenced_at);

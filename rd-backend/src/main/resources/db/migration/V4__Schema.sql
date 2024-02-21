CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE messenger_instance
    ADD COLUMN telematik_id  VARCHAR(255),
    ADD COLUMN profession_id VARCHAR(255),
    ADD COLUMN instance_id   VARCHAR(255);

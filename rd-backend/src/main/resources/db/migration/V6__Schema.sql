CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE org_admin
    ADD COLUMN server_name VARCHAR (255) NOT NULL,
    ADD CONSTRAINT fk_server_name FOREIGN KEY(server_name) REFERENCES messenger_instance(server_name);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS messenger_instance (
    id uuid DEFAULT uuid_generate_v4 (),
    version INT NOT NULL,
    server_name VARCHAR (255) unique,
    public_base_url VARCHAR (255) unique,
    user_id VARCHAR (255),
    PRIMARY KEY (id));

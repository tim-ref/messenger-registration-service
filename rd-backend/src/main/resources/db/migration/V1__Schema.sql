-- Enable uuid support
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- Creates the Schema for FederationList and Domain
CREATE TABLE IF NOT EXISTS federation_list (
    id uuid DEFAULT uuid_generate_v4 (),
    version INT NOT NULL,
    hash_algorithm VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS domain (
    id uuid DEFAULT uuid_generate_v4 (),
    domain VARCHAR (255),
    is_insurance boolean NOT NULL DEFAULT FALSE,
    telematik_id VARCHAR (255),
    federation_list_id uuid,
    PRIMARY KEY (id),
    CONSTRAINT fk_federation_list FOREIGN KEY(federation_list_id) REFERENCES federation_list(id)
);

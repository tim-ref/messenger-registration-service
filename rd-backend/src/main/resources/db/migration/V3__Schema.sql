CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS org_admin (
    id uuid DEFAULT uuid_generate_v4 (),
    telematik_id VARCHAR (255),
    mx_id VARCHAR (255) UNIQUE,
    profession_oid VARCHAR (255),
    PRIMARY KEY (id)
);

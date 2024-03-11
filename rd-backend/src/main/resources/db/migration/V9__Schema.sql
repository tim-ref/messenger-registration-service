CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE org_admin
    ALTER COLUMN telematik_id SET DEFAULT '',
    ALTER COLUMN telematik_id SET NOT NULL,

    ALTER COLUMN profession_oid SET DEFAULT '',
    ALTER COLUMN profession_oid SET NOT NULL,

    ALTER COLUMN mx_id SET DEFAULT '',
    ALTER COLUMN mx_id SET NOT NULL;

ALTER TABLE messenger_instance
    ALTER COLUMN telematik_id SET DEFAULT '',
    ALTER COLUMN telematik_id SET NOT NULL,

    ALTER COLUMN profession_id SET DEFAULT '',
    ALTER COLUMN profession_id SET NOT NULL,

    ALTER COLUMN user_id SET DEFAULT '',
    ALTER COLUMN user_id SET NOT NULL,

    ALTER COLUMN instance_id SET DEFAULT '',
    ALTER COLUMN instance_id SET NOT NULL;

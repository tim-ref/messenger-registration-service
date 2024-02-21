CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE messenger_instance
    ADD COLUMN date_of_order DATE,
    ADD COLUMN end_of_life_date DATE;

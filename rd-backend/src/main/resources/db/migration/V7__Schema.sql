ALTER TABLE messenger_instance
    ADD COLUMN active BOOLEAN default true,
    ADD COLUMN start_of_inactivity BIGINT;

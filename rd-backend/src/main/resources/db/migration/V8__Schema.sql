-- Enable uuid support
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Removes unused column 'hash_algorithm'
ALTER TABLE federation_list
    DROP COLUMN hash_algorithm;

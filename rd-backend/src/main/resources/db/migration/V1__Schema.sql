--
-- Copyright (C) 2023 akquinet GmbH
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

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

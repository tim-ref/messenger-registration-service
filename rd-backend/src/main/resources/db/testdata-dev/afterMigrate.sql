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

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','proxy-test.eu.timref.akquinet.nx2.dev', 'proxy-test.eu.timref.akquinet.nx2.dev', '1111-akq', '1.2.276.0.76.4.53', 'proxytesteu', 'user', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','proxy-test2.eu.timref.akquinet.nx2.dev', 'proxy-test2.eu.timref.akquinet.nx2.dev', '1112-akq', '1.2.276.0.76.4.53', 'proxytest2eu', 'user', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','localhost', 'localhost', '1113-akq', '1.2.276.0.76.4.53', 'localhost', 'user', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','arvato.tu.timref.akquinet.nx2.dev', 'arvato.tu.timref.akquinet.nx2.dev', '1114-akq', '1.2.276.0.76.4.53', 'arvatotu', 'user', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO org_admin
    (telematik_id, mx_id, profession_oid, server_name)
VALUES ('1112-akq', '@testuser:proxy-test2.eu.timref.akquinet.nx2.dev', '1.2.276.0.76.4.53', 'proxy-test2.eu.timref.akquinet.nx2.dev') ON CONFLICT
ON CONSTRAINT org_admin_mx_id_key DO NOTHING;

INSERT INTO org_admin
    (telematik_id, mx_id, profession_oid, server_name)
VALUES ('1113-akq', '@admin:localhost', '1.2.276.0.76.4.53', 'localhost') ON CONFLICT
ON CONSTRAINT org_admin_mx_id_key DO NOTHING;

INSERT INTO org_admin
    (telematik_id, mx_id, profession_oid, server_name)
VALUES ('1114-akq', '@46d90941772ac615:arvato.tu.timref.akquinet.nx2.dev', '1.2.276.0.76.4.53', 'arvato.tu.timref.akquinet.nx2.dev') ON CONFLICT
ON CONSTRAINT org_admin_mx_id_key DO NOTHING;

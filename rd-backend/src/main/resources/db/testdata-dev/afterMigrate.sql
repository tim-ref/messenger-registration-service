INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','proxy-test.timref.example.com', 'proxy-test.timref.example.com', '1111-akq', '1.2.276.0.76.4.53', 'proxytesteu', 'user', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','proxy-test2.timref.example.com', 'proxy-test2.timref.example.com', '1112-akq', '1.2.276.0.76.4.53', 'proxytest2eu', 'user', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','localhost', 'localhost', '1113-akq', '1.2.276.0.76.4.53', 'localhost', 'user', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','arvato.timref.example.com', 'arvato.timref.example.com', '1114-akq', '1.2.276.0.76.4.53', 'arvatotu', 'user', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO org_admin
    (telematik_id, mx_id, profession_oid, server_name)
VALUES ('1112-akq', '@testuser:proxy-test2.timref.example.com', '1.2.276.0.76.4.53', 'proxy-test2.timref.example.com') ON CONFLICT
ON CONSTRAINT org_admin_mx_id_key DO NOTHING;

INSERT INTO org_admin
    (telematik_id, mx_id, profession_oid, server_name)
VALUES ('1113-akq', '@admin:localhost', '1.2.276.0.76.4.53', 'localhost') ON CONFLICT
ON CONSTRAINT org_admin_mx_id_key DO NOTHING;

INSERT INTO org_admin
    (telematik_id, mx_id, profession_oid, server_name)
VALUES ('1114-akq', '@46d90941772ac615:arvato.timref.example.com', '1.2.276.0.76.4.53', 'arvato.timref.example.com') ON CONFLICT
ON CONSTRAINT org_admin_mx_id_key DO NOTHING;

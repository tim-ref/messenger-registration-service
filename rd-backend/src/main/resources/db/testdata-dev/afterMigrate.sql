INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','proxy-test.timref.example.com', 'proxy-test.timref.example.com', '1111-akq', '1.2.276.0.76.4.53', 'proxytesteu', 'test', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','proxy-test2.timref.example.com', 'proxy-test2.timref.example.com', '1112-akq', '1.2.276.0.76.4.53', 'proxytest2eu', 'test', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','localhost', 'localhost', '1113-akq', '1.2.276.0.76.4.53', 'localhost', 'test', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

INSERT INTO messenger_instance(version,server_name, public_base_url, telematik_id, profession_id, instance_id, user_id, date_of_order, end_of_life_date)
VALUES ('1','arvato.timref.example.com', 'arvato.timref.example.com', '1114-akq', '1.2.276.0.76.4.53', 'arvatotu', 'test', '2023-06-13', '2033-06-13') ON CONFLICT DO NOTHING;

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

INSERT INTO support_information (id, server_name, user_id, support_page)
VALUES ('f165fd18-7c11-419b-9b88-3859d0b3e35b', 'proxy-test.timref.example.com', 'test', 'http://support.example.com') ON CONFLICT DO NOTHING;

INSERT INTO support_contact (id, email_address, matrix_id, role, support_information_id)
VALUES ('dd6594c6-8dd8-45b1-9f26-3ab851703189', 'support@example.com', '@support:proxy-test.timref.example.com', 'm.role.admin', 'f165fd18-7c11-419b-9b88-3859d0b3e35b') ON CONFLICT DO NOTHING;

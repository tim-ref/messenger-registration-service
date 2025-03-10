CREATE TABLE support_contact
(
    id                     UUID NOT NULL,
    email_address          VARCHAR(255),
    matrix_id              VARCHAR(255),
    role                   VARCHAR(255),
    support_information_id UUID,
    CONSTRAINT pk_support_contact PRIMARY KEY (id)
);

CREATE TABLE support_information
(
    id           UUID NOT NULL,
    server_name  VARCHAR(255),
    user_id      VARCHAR(255),
    support_page VARCHAR(255),
    CONSTRAINT pk_support_information PRIMARY KEY (id)
);

ALTER TABLE support_information
    ADD CONSTRAINT uc_support_information_server_name UNIQUE (server_name);

ALTER TABLE support_contact
    ADD CONSTRAINT FK_SUPPORT_CONTACT_ON_SUPPORT_INFORMATION FOREIGN KEY (support_information_id) REFERENCES support_information (id);
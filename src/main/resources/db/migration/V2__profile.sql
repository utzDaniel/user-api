CREATE TABLE tb_profile (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    username        VARCHAR(255) NOT NULL,
    nome_completo   VARCHAR(255) NOT NULL,
    cpf             VARCHAR(14)  NULL,
    email           VARCHAR(255) NOT NULL,
    telefone        VARCHAR(20)  NULL,
    created_at      DATETIME2    NOT NULL DEFAULT GETUTCDATE(),
    updated_at      DATETIME2    NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT uq_profile_username UNIQUE (username),
    CONSTRAINT uq_profile_cpf      UNIQUE (cpf),
    CONSTRAINT uq_profile_email    UNIQUE (email)
);

CREATE TABLE tb_family (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    titular_profile_id  BIGINT    NOT NULL,
    nome                VARCHAR(100) NOT NULL,
    created_at          DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT fk_family_titular FOREIGN KEY (titular_profile_id) REFERENCES tb_profile(id)
);

CREATE TABLE tb_family_member (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    family_id   BIGINT       NOT NULL,
    profile_id  BIGINT       NOT NULL,
    parentesco  VARCHAR(50)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    joined_at   DATETIME2    NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT fk_member_family  FOREIGN KEY (family_id)  REFERENCES tb_family(id),
    CONSTRAINT fk_member_profile FOREIGN KEY (profile_id) REFERENCES tb_profile(id),
    CONSTRAINT uq_member_profile UNIQUE (profile_id),
    CONSTRAINT uq_member_family_profile UNIQUE (family_id, profile_id)
);

CREATE TABLE tb_family_invitation (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    family_id             BIGINT       NOT NULL,
    requester_profile_id  BIGINT       NOT NULL,
    receiver_email        VARCHAR(255) NOT NULL,
    parentesco            VARCHAR(50)  NOT NULL,
    status                VARCHAR(30)  NOT NULL DEFAULT 'AGUARDANDO_TITULAR',
    created_at            DATETIME2    NOT NULL DEFAULT GETUTCDATE(),
    updated_at            DATETIME2    NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT fk_invitation_family    FOREIGN KEY (family_id)            REFERENCES tb_family(id),
    CONSTRAINT fk_invitation_requester FOREIGN KEY (requester_profile_id) REFERENCES tb_profile(id)
);

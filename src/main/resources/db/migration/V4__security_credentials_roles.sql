-- =========================================================
-- Seguridad: credenciales (1:1 con users) y roles (N:1)
-- =========================================================
CREATE TABLE user_credentials (
    user_id              BIGINT       PRIMARY KEY,
    password_hash        VARCHAR(100) NOT NULL,
    password_changed_at  TIMESTAMP,

    CONSTRAINT fk_user_credentials_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id  BIGINT      NOT NULL,
    role     VARCHAR(20) NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_roles_role CHECK (role IN ('USER', 'STAFF', 'ADMIN'))
);

-- Datos legados: los usuarios existentes reciben el rol USER, pero NO se les
-- crea credencial. Sin password_hash no pueden iniciar sesión hasta una
-- activación segura posterior. No se inventa ninguna contraseña por defecto.
INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users;
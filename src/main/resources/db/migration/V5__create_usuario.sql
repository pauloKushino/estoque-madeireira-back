-- Usuarios que fazem login no sistema.
-- A senha NUNCA e guardada em texto puro: guardamos o hash BCrypt dela.

CREATE TABLE usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL,
    senha VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Usuarios iniciais para testar o login:
--   admin / admin123  -> role ADMIN
--   user  / user123   -> role USER
INSERT INTO usuario (username, nome, senha, role) VALUES
('admin', 'Administrador', '$2a$10$QSaGGfWGMtGHOqIGdeonfu2yDsgDRUKuWcynq9NNLeFH83Q8FaIyq', 'ADMIN'),
('user', 'Usuario Padrao', '$2a$10$IXfvvzOv7WaIS227VLL5jOwuttk3XWb/Fn4HSVOgy9QrVaasLbUgW', 'USER');

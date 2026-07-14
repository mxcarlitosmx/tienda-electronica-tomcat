CREATE DATABASE servicio_web;
USE servicio_web;

-- Tabla usuarios
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    password VARCHAR(64) NOT NULL,
    token VARCHAR(20),
    email VARCHAR(100) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(100) NOT NULL,
    apellido_materno VARCHAR(100),
    fecha_nacimiento DATETIME NOT NULL,
    telefono BIGINT,
    genero CHAR(1)
);

CREATE TABLE fotos_usuarios (
    id_foto INT AUTO_INCREMENT PRIMARY KEY,
    foto LONGBLOB,
    id_usuario INT NOT NULL
);

ALTER TABLE fotos_usuarios ADD FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario);
CREATE UNIQUE INDEX usuarios_1 ON usuarios(email);


-- RNF-1.1 Tabla stock
CREATE TABLE stock (
    id_articulo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    cantidad INT NOT NULL
);

-- RNF-1.2 Tabla fotos_articulos
CREATE TABLE fotos_articulos (
    id_foto INT AUTO_INCREMENT PRIMARY KEY,
    foto LONGBLOB NOT NULL,
    id_articulo INT NOT NULL,
    FOREIGN KEY (id_articulo) REFERENCES stock(id_articulo)
);

-- RNF-1.3 Tabla carrito_compra
CREATE TABLE carrito_compra (
    id_usuario INT NOT NULL,
    id_articulo INT NOT NULL,
    cantidad INT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_articulo) REFERENCES stock(id_articulo)
);

-- RNF-1.4 Indice único para evitar articulos duplicados en el mismo carrito
CREATE UNIQUE INDEX usuarios_articulos_idx ON carrito_compra(id_usuario, id_articulo);
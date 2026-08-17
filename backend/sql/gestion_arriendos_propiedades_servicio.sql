CREATE DATABASE IF NOT EXISTS gestion_arriendos
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE gestion_arriendos;

CREATE TABLE IF NOT EXISTS propiedades_servicio (
  id INT AUTO_INCREMENT PRIMARY KEY,
  titulo VARCHAR(150) NOT NULL,
  ciudad VARCHAR(100) NOT NULL,
  direccion VARCHAR(180) NOT NULL,
  precio_mensual DECIMAL(12,2) NOT NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'disponible',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO propiedades_servicio
  (titulo, ciudad, direccion, precio_mensual)
SELECT
  'Apartamento de prueba', 'Cartagena', 'Barrio Manga', 1200000
WHERE NOT EXISTS (
  SELECT 1 FROM propiedades_servicio WHERE titulo = 'Apartamento de prueba'
);

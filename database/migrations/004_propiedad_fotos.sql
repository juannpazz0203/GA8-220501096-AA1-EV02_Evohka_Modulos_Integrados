-- EVOHKA - Fotografias de inmuebles
-- Migracion versionada e idempotente para MySQL 8.4.
--
-- IMPORTANTE:
-- 1. Este archivo no ejecuta ningun cambio mientras la confirmacion permanezca
--    en PENDIENTE_AUTORIZACION.
-- 2. Debe importarse con gestion_arriendos seleccionada como base activa.
-- 3. Todas las referencias de tablas estan calificadas explicitamente.

SET @evohka_fotos_confirmar_ddl = 'PENDIENTE_AUTORIZACION';

SET @evohka_fotos_base_correcta = IF(
  CONVERT(DATABASE() USING utf8mb4) COLLATE utf8mb4_bin =
    _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin,
  1,
  0
);

SET @evohka_fotos_propiedades_compatible = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'propiedades' COLLATE utf8mb4_bin
    AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'id_propiedad' COLLATE utf8mb4_bin
    AND CONVERT(column_type USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'int' COLLATE utf8mb4_bin
    AND CONVERT(is_nullable USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'NO' COLLATE utf8mb4_bin
    AND CONVERT(column_key USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'PRI' COLLATE utf8mb4_bin
);

SET @evohka_fotos_tabla_existe = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'propiedad_fotos' COLLATE utf8mb4_bin
);

SET @evohka_fotos_tabla_innodb = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'propiedad_fotos' COLLATE utf8mb4_bin
    AND CONVERT(engine USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'InnoDB' COLLATE utf8mb4_bin
);

SET @evohka_fotos_columnas_compatibles = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'propiedad_fotos' COLLATE utf8mb4_bin
    AND (
      (column_name = 'id_foto' AND column_type = 'bigint unsigned'
        AND is_nullable = 'NO' AND column_key = 'PRI'
        AND extra = 'auto_increment')
      OR (column_name = 'id_propiedad' AND column_type = 'int'
        AND is_nullable = 'NO')
      OR (column_name = 'ruta_relativa' AND column_type = 'varchar(255)'
        AND is_nullable = 'NO')
      OR (column_name = 'mime_type' AND column_type = 'varchar(50)'
        AND is_nullable = 'NO')
      OR (column_name = 'tamano_bytes' AND column_type = 'int unsigned'
        AND is_nullable = 'NO')
      OR (column_name = 'orden' AND column_type = 'tinyint unsigned'
        AND is_nullable = 'NO')
      OR (column_name = 'fecha_creacion' AND data_type = 'datetime'
        AND datetime_precision = 3 AND is_nullable = 'NO')
    )
);

SET @evohka_fotos_columnas_total = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'propiedad_fotos' COLLATE utf8mb4_bin
);

SET @evohka_fotos_indice_propiedad = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = 'gestion_arriendos'
    AND table_name = 'propiedad_fotos'
    AND index_name = 'idx_propiedad_fotos_propiedad'
    AND seq_in_index = 1
    AND column_name = 'id_propiedad'
);

SET @evohka_fotos_unica_ruta = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = 'gestion_arriendos'
    AND table_name = 'propiedad_fotos'
    AND index_name = 'uq_propiedad_fotos_ruta'
    AND non_unique = 0
    AND seq_in_index = 1
    AND column_name = 'ruta_relativa'
);

SET @evohka_fotos_unico_orden = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = 'gestion_arriendos'
    AND table_name = 'propiedad_fotos'
    AND index_name = 'uq_propiedad_fotos_orden'
    AND non_unique = 0
    AND (
      (seq_in_index = 1 AND column_name = 'id_propiedad')
      OR (seq_in_index = 2 AND column_name = 'orden')
    )
);

SET @evohka_fotos_fk = (
  SELECT COUNT(*)
  FROM information_schema.referential_constraints AS rc
  INNER JOIN information_schema.key_column_usage AS kcu
    ON kcu.constraint_schema = rc.constraint_schema
   AND kcu.constraint_name = rc.constraint_name
   AND kcu.table_name = rc.table_name
  WHERE rc.constraint_schema = 'gestion_arriendos'
    AND rc.table_name = 'propiedad_fotos'
    AND rc.constraint_name = 'fk_propiedad_fotos_propiedad'
    AND rc.delete_rule = 'CASCADE'
    AND kcu.column_name = 'id_propiedad'
    AND kcu.referenced_table_schema = 'gestion_arriendos'
    AND kcu.referenced_table_name = 'propiedades'
    AND kcu.referenced_column_name = 'id_propiedad'
);

SET @evohka_fotos_tabla_compatible = IF(
  @evohka_fotos_tabla_existe = 0,
  1,
  IF(
    @evohka_fotos_tabla_existe = 1
      AND @evohka_fotos_tabla_innodb = 1
      AND @evohka_fotos_columnas_compatibles = 7
      AND @evohka_fotos_columnas_total = 7
      AND @evohka_fotos_indice_propiedad = 1
      AND @evohka_fotos_unica_ruta = 1
      AND @evohka_fotos_unico_orden = 2
      AND @evohka_fotos_fk = 1,
    1,
    0
  )
);

SET @evohka_fotos_autorizada = IF(
  @evohka_fotos_base_correcta = 1
    AND @evohka_fotos_propiedades_compatible = 1
    AND @evohka_fotos_tabla_compatible = 1
    AND CONVERT(@evohka_fotos_confirmar_ddl USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'AUTORIZO_CREAR_PROPIEDAD_FOTOS' COLLATE utf8mb4_bin,
  1,
  0
);

-- Guarda obligatoria anterior a cualquier DDL.
SET @evohka_fotos_guardar = IF(
  @evohka_fotos_autorizada = 1,
  'SELECT ''autorizacion_propiedad_fotos_validada'' AS estado',
  'SELECT EVOHKA_ABORTO_MIGRACION_PROPIEDAD_FOTOS()'
);
PREPARE evohka_fotos_sentencia_guardar FROM @evohka_fotos_guardar;
EXECUTE evohka_fotos_sentencia_guardar;
DEALLOCATE PREPARE evohka_fotos_sentencia_guardar;

SELECT
  'pre_ddl' AS etapa,
  @evohka_fotos_base_correcta AS base_correcta,
  @evohka_fotos_propiedades_compatible AS propiedades_compatible,
  @evohka_fotos_tabla_existe AS tabla_existe,
  @evohka_fotos_tabla_compatible AS tabla_compatible;

SET @evohka_fotos_crear = IF(
  @evohka_fotos_tabla_existe = 0,
  'CREATE TABLE `gestion_arriendos`.`propiedad_fotos` (
     `id_foto` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
     `id_propiedad` INT NOT NULL,
     `ruta_relativa` VARCHAR(255) NOT NULL,
     `mime_type` VARCHAR(50) NOT NULL,
     `tamano_bytes` INT UNSIGNED NOT NULL,
     `orden` TINYINT UNSIGNED NOT NULL,
     `fecha_creacion` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
     PRIMARY KEY (`id_foto`),
     UNIQUE KEY `uq_propiedad_fotos_ruta` (`ruta_relativa`),
     UNIQUE KEY `uq_propiedad_fotos_orden` (`id_propiedad`, `orden`),
     KEY `idx_propiedad_fotos_propiedad` (`id_propiedad`),
     CONSTRAINT `fk_propiedad_fotos_propiedad`
       FOREIGN KEY (`id_propiedad`)
       REFERENCES `gestion_arriendos`.`propiedades` (`id_propiedad`)
       ON DELETE CASCADE
   ) ENGINE=InnoDB
     DEFAULT CHARACTER SET utf8mb4
     COLLATE utf8mb4_general_ci',
  'SELECT ''propiedad_fotos ya existe y es compatible; DDL omitido'' AS resultado'
);
PREPARE evohka_fotos_sentencia_crear FROM @evohka_fotos_crear;
EXECUTE evohka_fotos_sentencia_crear;
DEALLOCATE PREPARE evohka_fotos_sentencia_crear;

-- Verificacion final agregada. No expone rutas ni datos de usuarios.
SELECT
  (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'gestion_arriendos'
      AND table_name = 'propiedad_fotos'
      AND engine = 'InnoDB'
  ) AS tabla_innodb,
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'gestion_arriendos'
      AND table_name = 'propiedad_fotos'
  ) AS columnas,
  (
    SELECT COUNT(*)
    FROM `gestion_arriendos`.`propiedad_fotos`
  ) AS fotografias_existentes;

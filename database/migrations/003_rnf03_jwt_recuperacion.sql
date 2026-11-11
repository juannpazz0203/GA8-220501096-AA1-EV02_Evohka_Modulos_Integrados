-- RNF03 / Bloque 2 / JWT y estructura de recuperacion
--
-- Migracion EXCLUSIVA para gestion_arriendos. Este archivo versionado queda
-- bloqueado deliberadamente: antes de una ejecucion autorizada se debe crear
-- una copia privada, completar las tres confirmaciones PENDIENTE y volver a
-- auditar el esquema. Debe importarse completo en una sola solicitud.
--
-- No almacena codigos ni modifica usuarios. Agrega la version de sesion con
-- valor inicial cero y crea la estructura vacia de recuperacion. MySQL realiza
-- confirmaciones implicitas alrededor de DDL; si una interrupcion ocurre entre
-- sentencias, la siguiente ejecucion segura reconoce lo ya creado.

SET @rnf03_b2_base_objetivo = 'gestion_arriendos';

-- Guardas fail-closed. No versionar una copia con estos valores habilitados.
SET @rnf03_b2_confirmar_base = 'PENDIENTE';
SET @rnf03_b2_confirmar_respaldo = 'PENDIENTE';
SET @rnf03_b2_confirmar_ddl = 'PENDIENTE';

SET @rnf03_b2_base_sesion_correcta = IF(
  CONVERT(COALESCE(DATABASE(), '') USING utf8mb4) COLLATE utf8mb4_bin =
    _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin,
  1,
  0
);
SET @rnf03_b2_base_existe = (
  SELECT COUNT(*)
  FROM information_schema.schemata
  WHERE CONVERT(schema_name USING utf8mb4) COLLATE utf8mb4_bin =
    _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
);
SET @rnf03_b2_usuarios_compatible = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
    AND CONVERT(engine USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'InnoDB' COLLATE utf8mb4_bin
);
SET @rnf03_b2_id_usuario_compatible = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
    AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'id_usuario' COLLATE utf8mb4_bin
    AND CONVERT(column_type USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'int' COLLATE utf8mb4_bin
    AND CONVERT(is_nullable USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'NO' COLLATE utf8mb4_bin
    AND CONVERT(column_key USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'PRI' COLLATE utf8mb4_bin
);
SET @rnf03_b2_contrasena_algoritmo_compatible = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
    AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'contrasena_algoritmo' COLLATE utf8mb4_bin
    AND CONVERT(data_type USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'varchar' COLLATE utf8mb4_bin
    AND character_maximum_length >= 32
    AND CONVERT(is_nullable USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'NO' COLLATE utf8mb4_bin
    AND CONVERT(COALESCE(column_default, '') USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
);

SET @rnf03_b2_sesion_columna_existe = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
    AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'sesion_version' COLLATE utf8mb4_bin
);
SET @rnf03_b2_sesion_columna_compatible = IF(
  @rnf03_b2_sesion_columna_existe = 0,
  1,
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
        _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
      AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
        _utf8mb4'usuarios' COLLATE utf8mb4_bin
      AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
        _utf8mb4'sesion_version' COLLATE utf8mb4_bin
      AND CONVERT(column_type USING utf8mb4) COLLATE utf8mb4_bin =
        _utf8mb4'int unsigned' COLLATE utf8mb4_bin
      AND CONVERT(is_nullable USING utf8mb4) COLLATE utf8mb4_bin =
        _utf8mb4'NO' COLLATE utf8mb4_bin
      AND CONVERT(COALESCE(column_default, '') USING utf8mb4)
        COLLATE utf8mb4_bin = _utf8mb4'0' COLLATE utf8mb4_bin
  )
);

SET @rnf03_b2_recuperacion_existe = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'recuperaciones_contrasena' COLLATE utf8mb4_bin
);
SET @rnf03_b2_recuperacion_innodb = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'recuperaciones_contrasena' COLLATE utf8mb4_bin
    AND CONVERT(engine USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'InnoDB' COLLATE utf8mb4_bin
);
SET @rnf03_b2_recuperacion_columnas = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'recuperaciones_contrasena' COLLATE utf8mb4_bin
    AND (
      (column_name = 'id_recuperacion' AND data_type = 'bigint'
        AND column_type = 'bigint unsigned' AND is_nullable = 'NO'
        AND column_key = 'PRI' AND extra = 'auto_increment')
      OR (column_name = 'id_usuario' AND column_type = 'int'
        AND is_nullable = 'NO')
      OR (column_name = 'codigo_hash' AND data_type = 'binary'
        AND character_maximum_length = 32 AND is_nullable = 'NO')
      OR (column_name = 'codigo_salt' AND data_type = 'binary'
        AND character_maximum_length = 16 AND is_nullable = 'NO')
      OR (column_name = 'fecha_creacion' AND data_type = 'datetime'
        AND datetime_precision = 3 AND is_nullable = 'NO')
      OR (column_name = 'fecha_expiracion' AND data_type = 'datetime'
        AND datetime_precision = 3 AND is_nullable = 'NO')
      OR (column_name = 'fecha_envio' AND data_type = 'datetime'
        AND datetime_precision = 3 AND is_nullable = 'YES')
      OR (column_name = 'intentos_fallidos'
        AND column_type = 'tinyint unsigned' AND is_nullable = 'NO'
        AND CONVERT(COALESCE(column_default, '') USING utf8mb4)
          COLLATE utf8mb4_bin = _utf8mb4'0' COLLATE utf8mb4_bin)
      OR (column_name = 'fecha_uso' AND data_type = 'datetime'
        AND datetime_precision = 3 AND is_nullable = 'YES')
      OR (column_name = 'fecha_revocacion' AND data_type = 'datetime'
        AND datetime_precision = 3 AND is_nullable = 'YES')
    )
);
SET @rnf03_b2_recuperacion_indice_usuario = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'recuperaciones_contrasena' COLLATE utf8mb4_bin
    AND CONVERT(index_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'idx_recuperacion_usuario_creacion' COLLATE utf8mb4_bin
    AND (
      (seq_in_index = 1 AND column_name = 'id_usuario')
      OR (seq_in_index = 2 AND column_name = 'fecha_creacion')
    )
);
SET @rnf03_b2_recuperacion_indice_expiracion = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'recuperaciones_contrasena' COLLATE utf8mb4_bin
    AND CONVERT(index_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'idx_recuperacion_expiracion' COLLATE utf8mb4_bin
    AND seq_in_index = 1
    AND column_name = 'fecha_expiracion'
);
SET @rnf03_b2_recuperacion_fk = (
  SELECT COUNT(*)
  FROM information_schema.referential_constraints AS rc
  INNER JOIN information_schema.key_column_usage AS kcu
    ON kcu.constraint_schema = rc.constraint_schema
   AND kcu.constraint_name = rc.constraint_name
   AND kcu.table_name = rc.table_name
  WHERE CONVERT(rc.constraint_schema USING utf8mb4)
      COLLATE utf8mb4_bin = _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(rc.table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'recuperaciones_contrasena' COLLATE utf8mb4_bin
    AND CONVERT(rc.constraint_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'fk_recuperacion_usuario' COLLATE utf8mb4_bin
    AND CONVERT(rc.delete_rule USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'CASCADE' COLLATE utf8mb4_bin
    AND kcu.column_name = 'id_usuario'
    AND kcu.referenced_table_schema = 'gestion_arriendos'
    AND kcu.referenced_table_name = 'usuarios'
    AND kcu.referenced_column_name = 'id_usuario'
);
SET @rnf03_b2_recuperacion_compatible = IF(
  @rnf03_b2_recuperacion_existe = 0,
  1,
  IF(
    @rnf03_b2_recuperacion_existe = 1
      AND @rnf03_b2_recuperacion_innodb = 1
      AND @rnf03_b2_recuperacion_columnas = 10
      AND @rnf03_b2_recuperacion_indice_usuario = 2
      AND @rnf03_b2_recuperacion_indice_expiracion = 1
      AND @rnf03_b2_recuperacion_fk = 1,
    1,
    0
  )
);

SET @rnf03_b2_autorizacion = IF(
  @rnf03_b2_base_sesion_correcta = 1
    AND @rnf03_b2_base_existe = 1
    AND @rnf03_b2_usuarios_compatible = 1
    AND @rnf03_b2_id_usuario_compatible = 1
    AND @rnf03_b2_contrasena_algoritmo_compatible = 1
    AND @rnf03_b2_sesion_columna_compatible = 1
    AND @rnf03_b2_recuperacion_compatible = 1
    AND CONVERT(@rnf03_b2_confirmar_base USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'AUTORIZO_RNF03_BLOQUE2_GESTION_ARRIENDOS'
          COLLATE utf8mb4_bin
    AND CONVERT(@rnf03_b2_confirmar_respaldo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'RESPALDO_RNF03_BLOQUE2_VERIFICADO' COLLATE utf8mb4_bin
    AND CONVERT(@rnf03_b2_confirmar_ddl USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'APLICAR_ESQUEMA_JWT_RECUPERACION_RNF03'
          COLLATE utf8mb4_bin,
  1,
  0
);

-- Esta guarda se ejecuta antes de cualquier DDL.
SET @rnf03_b2_guardar = IF(
  @rnf03_b2_autorizacion = 1,
  'SELECT ''autorizacion_rnf03_bloque2_validada'' AS estado',
  'SELECT RNF03_BLOQUE2_ABORTO_AUTORIZACION_O_ESQUEMA()'
);
PREPARE rnf03_b2_sentencia_guardar FROM @rnf03_b2_guardar;
EXECUTE rnf03_b2_sentencia_guardar;
DEALLOCATE PREPARE rnf03_b2_sentencia_guardar;

SELECT
  'pre_ddl' AS etapa,
  @rnf03_b2_base_sesion_correcta AS base_sesion_correcta,
  @rnf03_b2_contrasena_algoritmo_compatible
    AS contrasena_algoritmo_compatible,
  @rnf03_b2_sesion_columna_existe AS sesion_version_existe,
  @rnf03_b2_recuperacion_existe AS tabla_recuperacion_existe,
  @rnf03_b2_sesion_columna_compatible AS sesion_version_compatible,
  @rnf03_b2_recuperacion_compatible AS recuperacion_compatible;

SET @rnf03_b2_agregar_sesion = IF(
  @rnf03_b2_autorizacion <> 1,
  'SELECT RNF03_BLOQUE2_ABORTO_COLUMNA_NO_AUTORIZADA()',
  IF(
    @rnf03_b2_sesion_columna_existe = 0,
    'ALTER TABLE `gestion_arriendos`.`usuarios`
       ADD COLUMN `sesion_version` INT UNSIGNED NOT NULL DEFAULT 0
       AFTER `contrasena_algoritmo`',
    'SELECT ''sesion_version ya existe; DDL omitido'' AS resultado'
  )
);
PREPARE rnf03_b2_sentencia_sesion FROM @rnf03_b2_agregar_sesion;
EXECUTE rnf03_b2_sentencia_sesion;
DEALLOCATE PREPARE rnf03_b2_sentencia_sesion;

SET @rnf03_b2_crear_recuperacion = IF(
  @rnf03_b2_autorizacion <> 1,
  'SELECT RNF03_BLOQUE2_ABORTO_TABLA_NO_AUTORIZADA()',
  IF(
    @rnf03_b2_recuperacion_existe = 0,
    'CREATE TABLE `gestion_arriendos`.`recuperaciones_contrasena` (
       `id_recuperacion` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
       `id_usuario` INT NOT NULL,
       `codigo_hash` BINARY(32) NOT NULL,
       `codigo_salt` BINARY(16) NOT NULL,
       `fecha_creacion` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
       `fecha_expiracion` DATETIME(3) NOT NULL,
       `fecha_envio` DATETIME(3) NULL,
       `intentos_fallidos` TINYINT UNSIGNED NOT NULL DEFAULT 0,
       `fecha_uso` DATETIME(3) NULL,
       `fecha_revocacion` DATETIME(3) NULL,
       PRIMARY KEY (`id_recuperacion`),
       KEY `idx_recuperacion_usuario_creacion`
         (`id_usuario`, `fecha_creacion`),
       KEY `idx_recuperacion_expiracion` (`fecha_expiracion`),
       CONSTRAINT `fk_recuperacion_usuario`
         FOREIGN KEY (`id_usuario`)
         REFERENCES `usuarios` (`id_usuario`)
         ON DELETE CASCADE
     ) ENGINE=InnoDB
       DEFAULT CHARACTER SET utf8mb4
       COLLATE utf8mb4_general_ci',
    'SELECT ''recuperaciones_contrasena ya existe; DDL omitido'' AS resultado'
  )
);
PREPARE rnf03_b2_sentencia_recuperacion
  FROM @rnf03_b2_crear_recuperacion;
EXECUTE rnf03_b2_sentencia_recuperacion;
DEALLOCATE PREPARE rnf03_b2_sentencia_recuperacion;

-- Verificacion final agregada; no lee contrasenas, hashes ni codigos.
SELECT
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'gestion_arriendos'
      AND table_name = 'usuarios'
      AND column_name = 'sesion_version'
      AND column_type = 'int unsigned'
      AND is_nullable = 'NO'
      AND CONVERT(COALESCE(column_default, '') USING utf8mb4)
        COLLATE utf8mb4_bin = _utf8mb4'0' COLLATE utf8mb4_bin
  ) AS sesion_version_compatible,
  (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = 'gestion_arriendos'
      AND table_name = 'recuperaciones_contrasena'
      AND engine = 'InnoDB'
  ) AS tabla_recuperacion_compatible,
  (
    SELECT COUNT(*)
    FROM `gestion_arriendos`.`recuperaciones_contrasena`
  ) AS recuperaciones_existentes;

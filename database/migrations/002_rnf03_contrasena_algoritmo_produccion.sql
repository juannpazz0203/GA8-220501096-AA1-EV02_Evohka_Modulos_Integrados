-- RNF03 / Bloque 1 / Produccion
--
-- Migracion EXCLUSIVA para gestion_arriendos. El archivo versionado queda
-- deliberadamente bloqueado. Para una ejecucion autorizada se debe crear una
-- copia privada, reemplazar los valores PENDIENTE y los conteos -1 con los
-- resultados de la auditoria inmediata, e importar toda la copia en una sola
-- solicitud. No depende de variables definidas en sesiones anteriores.
--
-- Este SQL solo agrega y valida contrasena_algoritmo y clasifica hashes bcrypt
-- autocontenidos. Las credenciales legacy restantes se convierten despues con
-- el migrador Node.js protegido. Todas las referencias de usuarios estan
-- calificadas para no depender de cambios de contexto de phpMyAdmin.

SET @rnf03_prod_base_objetivo = 'gestion_arriendos';

-- Guardas fail-closed. No versionar una copia con estos valores habilitados.
SET @rnf03_prod_confirmar_base = 'PENDIENTE';
SET @rnf03_prod_confirmar_respaldo = 'PENDIENTE';
SET @rnf03_prod_confirmar_ddl = 'PENDIENTE';
SET @rnf03_prod_confirmar_clasificacion = 'PENDIENTE';
SET @rnf03_prod_candidatas_marcador_esperadas = -1;
SET @rnf03_prod_colisiones_marcador_confirmadas = -1;

SET @rnf03_prod_marcador_bcrypt = '{EVOHKA_BCRYPT_V1}';
SET @rnf03_prod_patron_bcrypt =
  '^[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}$';

SET @rnf03_prod_base_sesion_correcta = IF(
  CONVERT(COALESCE(DATABASE(), '') USING utf8mb4) COLLATE utf8mb4_bin =
    _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin,
  1,
  0
);
SET @rnf03_prod_base_existe = (
  SELECT COUNT(*)
  FROM information_schema.schemata
  WHERE CONVERT(schema_name USING utf8mb4) COLLATE utf8mb4_bin =
    _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
);
SET @rnf03_prod_tabla_usuarios_existe = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
);
SET @rnf03_prod_usuarios_innodb = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
    AND CONVERT(engine USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'InnoDB' COLLATE utf8mb4_bin
);
SET @rnf03_prod_id_usuario_compatible = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
    AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'id_usuario' COLLATE utf8mb4_bin
    AND CONVERT(data_type USING utf8mb4) COLLATE utf8mb4_bin IN (
      _utf8mb4'int' COLLATE utf8mb4_bin,
      _utf8mb4'bigint' COLLATE utf8mb4_bin,
      _utf8mb4'mediumint' COLLATE utf8mb4_bin,
      _utf8mb4'smallint' COLLATE utf8mb4_bin
    )
    AND CONVERT(is_nullable USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'NO' COLLATE utf8mb4_bin
    AND CONVERT(column_key USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'PRI' COLLATE utf8mb4_bin
);
SET @rnf03_prod_contrasena_compatible = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
    AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'contrasena' COLLATE utf8mb4_bin
    AND CONVERT(data_type USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'varchar' COLLATE utf8mb4_bin
    AND character_maximum_length >= 78
    AND CONVERT(is_nullable USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'NO' COLLATE utf8mb4_bin
);
SET @rnf03_prod_columna_existe = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
    AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'contrasena_algoritmo' COLLATE utf8mb4_bin
);
SET @rnf03_prod_columna_compatible_antes = IF(
  @rnf03_prod_columna_existe = 0,
  1,
  (
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
      AND CONVERT(column_default USING utf8mb4) COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
  )
);

SET @rnf03_prod_autorizacion_operativa = IF(
  @rnf03_prod_base_sesion_correcta = 1
    AND @rnf03_prod_base_existe = 1
    AND @rnf03_prod_tabla_usuarios_existe = 1
    AND @rnf03_prod_usuarios_innodb = 1
    AND @rnf03_prod_id_usuario_compatible = 1
    AND @rnf03_prod_contrasena_compatible = 1
    AND @rnf03_prod_columna_compatible_antes = 1
    AND CONVERT(@rnf03_prod_confirmar_base USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'AUTORIZO_GESTION_ARRIENDOS_RNF03' COLLATE utf8mb4_bin
    AND CONVERT(@rnf03_prod_confirmar_respaldo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'RESPALDO_RNF03_PRODUCCION_VERIFICADO'
          COLLATE utf8mb4_bin
    AND CONVERT(@rnf03_prod_confirmar_ddl USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'APLICAR_COLUMNA_RNF03_PRODUCCION' COLLATE utf8mb4_bin
    AND CONVERT(@rnf03_prod_confirmar_clasificacion USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'CLASIFICAR_MARCADORES_RNF03_PRODUCCION'
          COLLATE utf8mb4_bin
    AND @rnf03_prod_candidatas_marcador_esperadas >= 0
    AND @rnf03_prod_colisiones_marcador_confirmadas = 0,
  1,
  0
);

-- Primera guarda: se evalua antes de leer credenciales o ejecutar DDL/DML.
SET @rnf03_prod_guardar_operacion = IF(
  @rnf03_prod_autorizacion_operativa = 1,
  'SELECT ''autorizacion_produccion_rnf03_validada'' AS estado',
  'SELECT RNF03_ABORTO_PRODUCCION_AUTORIZACION_INCOMPLETA()'
);
PREPARE rnf03_prod_sentencia_guardar
  FROM @rnf03_prod_guardar_operacion;
EXECUTE rnf03_prod_sentencia_guardar;
DEALLOCATE PREPARE rnf03_prod_sentencia_guardar;

SET @rnf03_prod_prefijos_antes = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE CONVERT(
    LEFT(contrasena, CHAR_LENGTH(@rnf03_prod_marcador_bcrypt))
    USING utf8mb4
  ) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_prod_marcador_bcrypt USING utf8mb4)
      COLLATE utf8mb4_bin
);
SET @rnf03_prod_candidatas_antes = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE CONVERT(
    LEFT(contrasena, CHAR_LENGTH(@rnf03_prod_marcador_bcrypt))
    USING utf8mb4
  ) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_prod_marcador_bcrypt USING utf8mb4)
      COLLATE utf8mb4_bin
    AND REGEXP_LIKE(
      CONVERT(
        SUBSTRING(
          contrasena,
          CHAR_LENGTH(@rnf03_prod_marcador_bcrypt) + 1
        ) USING utf8mb4
      ) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_prod_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
);
SET @rnf03_prod_estado_marcadores_previo_seguro = IF(
  (
    @rnf03_prod_prefijos_antes = 0
    AND @rnf03_prod_candidatas_antes = 0
  )
  OR (
    @rnf03_prod_prefijos_antes = @rnf03_prod_candidatas_antes
    AND @rnf03_prod_candidatas_antes =
      @rnf03_prod_candidatas_marcador_esperadas
  ),
  1,
  0
);

SELECT
  'pre_ddl_produccion' AS etapa,
  @rnf03_prod_base_sesion_correcta AS base_sesion_correcta,
  @rnf03_prod_columna_existe AS columna_algoritmo_existe,
  @rnf03_prod_prefijos_antes AS credenciales_con_prefijo,
  @rnf03_prod_candidatas_antes AS marcadores_bcrypt_validos,
  @rnf03_prod_candidatas_marcador_esperadas AS marcadores_esperados,
  @rnf03_prod_colisiones_marcador_confirmadas AS colisiones_confirmadas;

-- Segunda guarda: cualquier diferencia de marcadores aborta antes del DDL.
SET @rnf03_prod_agregar_columna = IF(
  @rnf03_prod_autorizacion_operativa <> 1
    OR @rnf03_prod_estado_marcadores_previo_seguro <> 1,
  'SELECT RNF03_ABORTO_PRODUCCION_ESTADO_MARCADORES_INSEGURO()',
  IF(
    @rnf03_prod_columna_existe = 0,
    'ALTER TABLE `gestion_arriendos`.`usuarios` ADD COLUMN contrasena_algoritmo VARCHAR(32) NOT NULL DEFAULT ''legacy_plain'' AFTER contrasena',
    'SELECT ''contrasena_algoritmo ya existe; DDL omitido'' AS resultado'
  )
);
PREPARE rnf03_prod_sentencia_columna
  FROM @rnf03_prod_agregar_columna;
EXECUTE rnf03_prod_sentencia_columna;
DEALLOCATE PREPARE rnf03_prod_sentencia_columna;

SET @rnf03_prod_columna_compatible = (
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
    AND CONVERT(column_default USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
);
SET @rnf03_prod_algoritmos_desconocidos = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE contrasena_algoritmo IS NULL
    OR CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin NOT IN (
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin,
        _utf8mb4'bcrypt' COLLATE utf8mb4_bin
      )
);
SET @rnf03_prod_prefijos_actuales = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE CONVERT(
    LEFT(contrasena, CHAR_LENGTH(@rnf03_prod_marcador_bcrypt))
    USING utf8mb4
  ) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_prod_marcador_bcrypt USING utf8mb4)
      COLLATE utf8mb4_bin
);
SET @rnf03_prod_candidatas_actuales = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
    AND CONVERT(
      LEFT(contrasena, CHAR_LENGTH(@rnf03_prod_marcador_bcrypt))
      USING utf8mb4
    ) COLLATE utf8mb4_bin =
      CONVERT(@rnf03_prod_marcador_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin
    AND REGEXP_LIKE(
      CONVERT(
        SUBSTRING(
          contrasena,
          CHAR_LENGTH(@rnf03_prod_marcador_bcrypt) + 1
        ) USING utf8mb4
      ) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_prod_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
);
SET @rnf03_prod_bcrypt_invalidas = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin = _utf8mb4'bcrypt' COLLATE utf8mb4_bin
    AND NOT REGEXP_LIKE(
      CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_prod_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
);
SET @rnf03_prod_legacy_mayores_72_bytes = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
    AND OCTET_LENGTH(contrasena) > 72
    AND CONVERT(
      LEFT(contrasena, CHAR_LENGTH(@rnf03_prod_marcador_bcrypt))
      USING utf8mb4
    ) COLLATE utf8mb4_bin <>
      CONVERT(@rnf03_prod_marcador_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin
);
SET @rnf03_prod_legacy_bcrypt_sin_marcador = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
    AND REGEXP_LIKE(
      CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_prod_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
);
SET @rnf03_prod_credenciales_vacias = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE contrasena IS NULL OR OCTET_LENGTH(contrasena) = 0
);
SET @rnf03_prod_estado_marcadores_seguro = IF(
  (
    @rnf03_prod_prefijos_actuales = 0
    AND @rnf03_prod_candidatas_actuales = 0
  )
  OR (
    @rnf03_prod_prefijos_actuales = @rnf03_prod_candidatas_actuales
    AND @rnf03_prod_candidatas_actuales =
      @rnf03_prod_candidatas_marcador_esperadas
  ),
  1,
  0
);
SET @rnf03_prod_datos_seguros = IF(
  @rnf03_prod_autorizacion_operativa = 1
    AND @rnf03_prod_columna_compatible = 1
    AND @rnf03_prod_algoritmos_desconocidos = 0
    AND @rnf03_prod_bcrypt_invalidas = 0
    AND @rnf03_prod_legacy_mayores_72_bytes = 0
    AND @rnf03_prod_legacy_bcrypt_sin_marcador = 0
    AND @rnf03_prod_credenciales_vacias = 0
    AND @rnf03_prod_estado_marcadores_seguro = 1
    AND @rnf03_prod_colisiones_marcador_confirmadas = 0,
  1,
  0
);

SELECT
  'antes_de_clasificar_produccion' AS etapa,
  @rnf03_prod_algoritmos_desconocidos AS algoritmos_desconocidos,
  @rnf03_prod_prefijos_actuales AS credenciales_con_prefijo,
  @rnf03_prod_candidatas_actuales AS marcadores_bcrypt_validos,
  @rnf03_prod_bcrypt_invalidas AS bcrypt_invalidas,
  @rnf03_prod_legacy_mayores_72_bytes AS legacy_mayores_72_bytes,
  @rnf03_prod_legacy_bcrypt_sin_marcador AS legacy_bcrypt_sin_marcador,
  @rnf03_prod_credenciales_vacias AS credenciales_vacias;

SET @rnf03_prod_clasificar_marcadores = IF(
  @rnf03_prod_datos_seguros <> 1,
  'SELECT RNF03_ABORTO_PRODUCCION_DATOS_NO_MIGRABLES()',
  IF(
    @rnf03_prod_candidatas_actuales = 0,
    'SELECT ''No hay marcadores pendientes; clasificacion omitida'' AS resultado',
    'UPDATE `gestion_arriendos`.`usuarios`
     SET contrasena = SUBSTRING(
           contrasena,
           CHAR_LENGTH(@rnf03_prod_marcador_bcrypt) + 1
         ),
         contrasena_algoritmo = ''bcrypt''
     WHERE CONVERT(contrasena_algoritmo USING utf8mb4)
         COLLATE utf8mb4_bin =
           _utf8mb4''legacy_plain'' COLLATE utf8mb4_bin
       AND CONVERT(
         LEFT(contrasena, CHAR_LENGTH(@rnf03_prod_marcador_bcrypt))
         USING utf8mb4
       ) COLLATE utf8mb4_bin =
         CONVERT(@rnf03_prod_marcador_bcrypt USING utf8mb4)
           COLLATE utf8mb4_bin
       AND REGEXP_LIKE(
         CONVERT(
           SUBSTRING(
             contrasena,
             CHAR_LENGTH(@rnf03_prod_marcador_bcrypt) + 1
           ) USING utf8mb4
         ) COLLATE utf8mb4_bin,
         CONVERT(@rnf03_prod_patron_bcrypt USING utf8mb4)
           COLLATE utf8mb4_bin,
         ''c''
       )'
  )
);
PREPARE rnf03_prod_sentencia_clasificacion
  FROM @rnf03_prod_clasificar_marcadores;
EXECUTE rnf03_prod_sentencia_clasificacion;
SET @rnf03_prod_filas_clasificadas = IF(
  @rnf03_prod_datos_seguros = 1
    AND @rnf03_prod_candidatas_actuales > 0,
  ROW_COUNT(),
  0
);
DEALLOCATE PREPARE rnf03_prod_sentencia_clasificacion;

-- Verificacion agregada: no devuelve contrasenas ni hashes.
SELECT
  COUNT(*) AS usuarios_total,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
  ), 0) AS credenciales_legacy,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin = _utf8mb4'bcrypt' COLLATE utf8mb4_bin
  ), 0) AS credenciales_bcrypt,
  COALESCE(SUM(
    contrasena_algoritmo IS NULL
    OR CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin NOT IN (
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin,
        _utf8mb4'bcrypt' COLLATE utf8mb4_bin
      )
  ), 0) AS algoritmos_desconocidos,
  COALESCE(SUM(
    CONVERT(
      LEFT(contrasena, CHAR_LENGTH(@rnf03_prod_marcador_bcrypt))
      USING utf8mb4
    ) COLLATE utf8mb4_bin =
      CONVERT(@rnf03_prod_marcador_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin
  ), 0) AS credenciales_con_marcador,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin = _utf8mb4'bcrypt' COLLATE utf8mb4_bin
    AND REGEXP_LIKE(
      CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_prod_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
  ), 0) AS bcrypt_validas,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin = _utf8mb4'bcrypt' COLLATE utf8mb4_bin
    AND NOT REGEXP_LIKE(
      CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_prod_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
  ), 0) AS bcrypt_invalidas,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
    AND OCTET_LENGTH(contrasena) > 72
  ), 0) AS legacy_mayores_72_bytes,
  @rnf03_prod_filas_clasificadas AS filas_clasificadas
FROM `gestion_arriendos`.`usuarios`;

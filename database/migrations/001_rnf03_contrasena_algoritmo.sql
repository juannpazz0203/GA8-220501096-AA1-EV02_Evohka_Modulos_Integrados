-- RNF03 / Bloque 1
--
-- Esta migración agrega la clasificación de credenciales y normaliza, sin
-- volver a calcular el hash, las credenciales autocontenidas creadas durante
-- la compatibilidad con el esquema antiguo.
--
-- Esta versión es EXCLUSIVA para validación. Solo permite modificar
-- gestion_arriendos_respaldo_test. La habilitación de producción requiere una
-- migración posterior revisada expresamente; no se obtiene cambiando variables
-- de sesión en este archivo.
--
-- La auditoría previa confirmó cero colisiones y cero credenciales marcadas en
-- la restauración. Esos valores se fijan dentro de este mismo archivo para no
-- depender de variables conservadas entre solicitudes distintas de
-- phpMyAdmin. Si el estado cambió, las guardas de conteo omiten la clasificación.
-- MySQL confirma implícitamente ALTER TABLE; aplicar en una ventana controlada
-- y únicamente después de verificar el respaldo y su restauración.

SET @rnf03_base_validacion_permitida =
  'gestion_arriendos_respaldo_test';
SET @rnf03_base_objetivo =
  'gestion_arriendos_respaldo_test';
SET @rnf03_base_objetivo_existe = (
  SELECT COUNT(*)
  FROM information_schema.schemata
  WHERE CONVERT(schema_name USING utf8mb4) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_base_objetivo USING utf8mb4) COLLATE utf8mb4_bin
);
SET @rnf03_base_objetivo_autorizada = IF(
  CONVERT(@rnf03_base_objetivo USING utf8mb4) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_base_validacion_permitida USING utf8mb4)
      COLLATE utf8mb4_bin
    AND @rnf03_base_objetivo_existe = 1,
  1,
  0
);

-- MySQL no admite IF/SIGNAL fuera de un programa almacenado. Esta sentencia
-- preparada provoca un error explícito si la base seleccionada no es la de
-- validación. Las dos mutaciones posteriores repiten además esta guarda, por
-- lo que no pueden ejecutarse aunque el cliente intente continuar tras error.
SET @rnf03_guardar_base_objetivo = IF(
  @rnf03_base_objetivo_autorizada = 1,
  'SELECT ''base_objetivo_rnf03_validada'' AS estado',
  'SELECT RNF03_ABORTO_BASE_OBJETIVO_NO_AUTORIZADA()'
);

PREPARE rnf03_sentencia_guardar_base
  FROM @rnf03_guardar_base_objetivo;
EXECUTE rnf03_sentencia_guardar_base;
DEALLOCATE PREPARE rnf03_sentencia_guardar_base;

SET @rnf03_marcador_bcrypt = '{EVOHKA_BCRYPT_V1}';
SET @rnf03_patron_bcrypt =
  '^[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}$';

SET @rnf03_colisiones_legacy_marcador = 0;
SET @rnf03_candidatas_marcador_esperadas = 0;
SET @rnf03_autorizar_clasificacion_marcador = 1;

SET @rnf03_prefijo_antes = (
  SELECT COUNT(*)
  FROM `gestion_arriendos_respaldo_test`.`usuarios`
  WHERE CONVERT(
    LEFT(contrasena, CHAR_LENGTH(@rnf03_marcador_bcrypt))
    USING utf8mb4
  ) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_marcador_bcrypt USING utf8mb4)
      COLLATE utf8mb4_bin
);

SET @rnf03_candidatas_antes = (
  SELECT COUNT(*)
  FROM `gestion_arriendos_respaldo_test`.`usuarios`
  WHERE CONVERT(
    LEFT(contrasena, CHAR_LENGTH(@rnf03_marcador_bcrypt))
    USING utf8mb4
  ) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_marcador_bcrypt USING utf8mb4)
      COLLATE utf8mb4_bin
    AND REGEXP_LIKE(
      CONVERT(
        SUBSTRING(
          contrasena,
          CHAR_LENGTH(@rnf03_marcador_bcrypt) + 1
        ) USING utf8mb4
      ) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
);

SELECT
  'antes_de_agregar_columna' AS etapa,
  @rnf03_prefijo_antes AS credenciales_con_prefijo,
  @rnf03_candidatas_antes AS credenciales_marcadas_bcrypt_validas;

SET @rnf03_columna_existe = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
      CONVERT(@rnf03_base_objetivo USING utf8mb4) COLLATE utf8mb4_bin
    AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'usuarios' COLLATE utf8mb4_bin
    AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
      _utf8mb4'contrasena_algoritmo' COLLATE utf8mb4_bin
);

SET @rnf03_agregar_columna = IF(
  @rnf03_base_objetivo_autorizada <> 1,
  'SELECT RNF03_ABORTO_DDL_BASE_OBJETIVO_NO_AUTORIZADA()',
  IF(
    @rnf03_columna_existe = 0,
    'ALTER TABLE `gestion_arriendos_respaldo_test`.`usuarios` ADD COLUMN contrasena_algoritmo VARCHAR(32) NOT NULL DEFAULT ''legacy_plain'' AFTER contrasena',
    'SELECT ''contrasena_algoritmo ya existe; no se realizaron cambios de esquema'' AS resultado'
  )
);

PREPARE rnf03_sentencia_columna FROM @rnf03_agregar_columna;
EXECUTE rnf03_sentencia_columna;
DEALLOCATE PREPARE rnf03_sentencia_columna;

SET @rnf03_prefijo_actual = (
  SELECT COUNT(*)
  FROM `gestion_arriendos_respaldo_test`.`usuarios`
  WHERE CONVERT(
    LEFT(contrasena, CHAR_LENGTH(@rnf03_marcador_bcrypt))
    USING utf8mb4
  ) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_marcador_bcrypt USING utf8mb4)
      COLLATE utf8mb4_bin
);

SET @rnf03_candidatas_actuales = (
  SELECT COUNT(*)
  FROM `gestion_arriendos_respaldo_test`.`usuarios`
  WHERE CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
    AND CONVERT(
      LEFT(contrasena, CHAR_LENGTH(@rnf03_marcador_bcrypt))
      USING utf8mb4
    ) COLLATE utf8mb4_bin =
      CONVERT(@rnf03_marcador_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin
    AND REGEXP_LIKE(
      CONVERT(
        SUBSTRING(
          contrasena,
          CHAR_LENGTH(@rnf03_marcador_bcrypt) + 1
        ) USING utf8mb4
      ) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
);

SET @rnf03_clasificacion_segura = IF(
  @rnf03_base_objetivo_autorizada = 1
    AND @rnf03_autorizar_clasificacion_marcador = 1
    AND @rnf03_colisiones_legacy_marcador = 0
    AND @rnf03_candidatas_marcador_esperadas >= 0
    AND @rnf03_candidatas_marcador_esperadas = @rnf03_candidatas_actuales
    AND @rnf03_candidatas_antes = @rnf03_candidatas_actuales
    AND @rnf03_prefijo_antes = @rnf03_prefijo_actual
    AND @rnf03_prefijo_actual = @rnf03_candidatas_actuales,
  1,
  0
);

SELECT
  'antes_de_clasificar' AS etapa,
  @rnf03_prefijo_actual AS credenciales_con_prefijo,
  @rnf03_candidatas_actuales AS candidatas_bcrypt_legacy,
  @rnf03_candidatas_marcador_esperadas AS candidatas_esperadas,
  @rnf03_colisiones_legacy_marcador AS colisiones_confirmadas,
  IF(
    @rnf03_clasificacion_segura = 1,
    'clasificacion_autorizada',
    'clasificacion_omitida_por_guardas'
  ) AS estado;

SET @rnf03_clasificar_marcadores = IF(
  @rnf03_base_objetivo_autorizada <> 1,
  'SELECT RNF03_ABORTO_DML_BASE_OBJETIVO_NO_AUTORIZADA()',
  IF(
    @rnf03_clasificacion_segura = 1,
    'UPDATE `gestion_arriendos_respaldo_test`.`usuarios`
   SET contrasena = SUBSTRING(
         contrasena,
         CHAR_LENGTH(@rnf03_marcador_bcrypt) + 1
       ),
       contrasena_algoritmo = ''bcrypt''
   WHERE CONVERT(contrasena_algoritmo USING utf8mb4)
       COLLATE utf8mb4_bin =
         _utf8mb4''legacy_plain'' COLLATE utf8mb4_bin
     AND CONVERT(
       LEFT(contrasena, CHAR_LENGTH(@rnf03_marcador_bcrypt))
       USING utf8mb4
     ) COLLATE utf8mb4_bin =
       CONVERT(@rnf03_marcador_bcrypt USING utf8mb4)
         COLLATE utf8mb4_bin
     AND REGEXP_LIKE(
       CONVERT(
         SUBSTRING(
           contrasena,
           CHAR_LENGTH(@rnf03_marcador_bcrypt) + 1
         ) USING utf8mb4
       ) COLLATE utf8mb4_bin,
       CONVERT(@rnf03_patron_bcrypt USING utf8mb4)
         COLLATE utf8mb4_bin,
       ''c''
     )',
    'SELECT ''No se modificaron credenciales: clasificación no autorizada o comprobaciones no coincidentes'' AS resultado'
  )
);

PREPARE rnf03_sentencia_clasificacion
  FROM @rnf03_clasificar_marcadores;
EXECUTE rnf03_sentencia_clasificacion;
SET @rnf03_filas_clasificadas = IF(
  @rnf03_clasificacion_segura = 1,
  ROW_COUNT(),
  0
);
DEALLOCATE PREPARE rnf03_sentencia_clasificacion;

SET @rnf03_marcadas_pendientes = (
  SELECT COUNT(*)
  FROM `gestion_arriendos_respaldo_test`.`usuarios`
  WHERE CONVERT(
    LEFT(contrasena, CHAR_LENGTH(@rnf03_marcador_bcrypt))
    USING utf8mb4
  ) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_marcador_bcrypt USING utf8mb4)
      COLLATE utf8mb4_bin
    AND REGEXP_LIKE(
      CONVERT(
        SUBSTRING(
          contrasena,
          CHAR_LENGTH(@rnf03_marcador_bcrypt) + 1
        ) USING utf8mb4
      ) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
);

SET @rnf03_bcrypt_limpias = (
  SELECT COUNT(*)
  FROM `gestion_arriendos_respaldo_test`.`usuarios`
  WHERE CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin = _utf8mb4'bcrypt' COLLATE utf8mb4_bin
    AND REGEXP_LIKE(
      CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
      CONVERT(@rnf03_patron_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin,
      'c'
    )
);

SET @rnf03_legacy_pendientes = (
  SELECT COUNT(*)
  FROM `gestion_arriendos_respaldo_test`.`usuarios`
  WHERE CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
);

SELECT
  'despues_de_clasificar' AS etapa,
  @rnf03_filas_clasificadas AS filas_clasificadas,
  @rnf03_marcadas_pendientes AS credenciales_marcadas_pendientes,
  @rnf03_bcrypt_limpias AS credenciales_bcrypt_limpias,
  @rnf03_legacy_pendientes AS credenciales_legacy_pendientes;

-- Verificaciones agregadas. No seleccionan contraseñas ni hashes.
SELECT
  column_name,
  column_type,
  is_nullable,
  column_default
FROM information_schema.columns
WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
    CONVERT(@rnf03_base_objetivo USING utf8mb4) COLLATE utf8mb4_bin
  AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
    _utf8mb4'usuarios' COLLATE utf8mb4_bin
  AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
    _utf8mb4'contrasena_algoritmo' COLLATE utf8mb4_bin;

SELECT
  COUNT(*) AS usuarios_total,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
  ), 0)
    AS credenciales_legacy,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin = _utf8mb4'bcrypt' COLLATE utf8mb4_bin
  ), 0)
    AS credenciales_bcrypt,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin NOT IN (
        _utf8mb4'legacy_plain' COLLATE utf8mb4_bin,
        _utf8mb4'bcrypt' COLLATE utf8mb4_bin
      )
  ), 0) AS algoritmos_desconocidos,
  COALESCE(SUM(
    CONVERT(
      LEFT(contrasena, CHAR_LENGTH(@rnf03_marcador_bcrypt))
      USING utf8mb4
    ) COLLATE utf8mb4_bin =
      CONVERT(@rnf03_marcador_bcrypt USING utf8mb4)
        COLLATE utf8mb4_bin
  ), 0) AS credenciales_con_marcador,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin = _utf8mb4'bcrypt' COLLATE utf8mb4_bin
      AND REGEXP_LIKE(
        CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
        CONVERT(@rnf03_patron_bcrypt USING utf8mb4)
          COLLATE utf8mb4_bin,
        'c'
      )
  ), 0) AS bcrypt_validas,
  COALESCE(SUM(
    CONVERT(contrasena_algoritmo USING utf8mb4)
      COLLATE utf8mb4_bin = _utf8mb4'bcrypt' COLLATE utf8mb4_bin
      AND NOT REGEXP_LIKE(
        CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
        CONVERT(@rnf03_patron_bcrypt USING utf8mb4)
          COLLATE utf8mb4_bin,
        'c'
      )
  ), 0) AS bcrypt_invalidas
FROM `gestion_arriendos_respaldo_test`.`usuarios`;

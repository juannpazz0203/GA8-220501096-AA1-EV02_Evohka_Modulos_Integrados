-- EVOHKA - Integridad del correo de usuarios
-- Migracion versionada e idempotente para MySQL 8.4.
--
-- ESTADO: PREPARADA, NO APLICADA.
--
-- IMPORTANTE:
-- 1. Este archivo versionado permanece bloqueado. No cambie las variables de
--    autorizacion dentro del repositorio.
-- 2. Antes de ejecutarlo, restaure y verifique un respaldo en una base aislada.
-- 3. Cree una copia privada fuera del repositorio y complete alli las tres
--    confirmaciones y los conteos obtenidos inmediatamente antes del DDL.
-- 4. Debe importarse con gestion_arriendos seleccionada como base activa.
-- 5. No corrige, normaliza ni elimina datos. Cualquier duplicado o correo no
--    canonico aborta la migracion antes del DDL.

SET @evohka_correo_confirmar_base = 'PENDIENTE';
SET @evohka_correo_confirmar_respaldo = 'PENDIENTE';
SET @evohka_correo_confirmar_ddl = 'PENDIENTE';
SET @evohka_correo_usuarios_esperados = -1;
SET @evohka_correo_duplicados_esperados = -1;
SET @evohka_correo_no_canonicos_esperados = -1;

SET @evohka_correo_base_correcta = IF(
  CONVERT(COALESCE(DATABASE(), '') USING utf8mb4) COLLATE utf8mb4_bin =
    _utf8mb4'gestion_arriendos' COLLATE utf8mb4_bin,
  1,
  0
);

SET @evohka_correo_tabla_compatible = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = 'gestion_arriendos'
    AND table_name = 'usuarios'
    AND engine = 'InnoDB'
);

SET @evohka_correo_columna_compatible = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = 'gestion_arriendos'
    AND table_name = 'usuarios'
    AND column_name = 'correo'
    AND column_type = 'varchar(150)'
    AND is_nullable = 'NO'
    AND character_set_name = 'utf8mb4'
    AND collation_name = 'utf8mb4_general_ci'
);

SET @evohka_correo_usuarios_actuales = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
);

-- Cuenta grupos, no filas sobrantes. La identidad vigente es LOWER(TRIM()).
SET @evohka_correo_duplicados_actuales = (
  SELECT COUNT(*)
  FROM (
    SELECT LOWER(TRIM(correo)) AS correo_normalizado
    FROM `gestion_arriendos`.`usuarios`
    GROUP BY LOWER(TRIM(correo))
    HAVING COUNT(*) > 1
  ) AS correos_duplicados
);

-- La comparacion binaria detecta mayusculas y espacios exteriores que una
-- comparacion con utf8mb4_general_ci podria considerar equivalentes.
SET @evohka_correo_no_canonicos_actuales = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE CAST(correo AS BINARY) <>
      CAST(LOWER(TRIM(correo)) AS BINARY)
    OR correo REGEXP '[[:space:]]'
);

SET @evohka_correo_indice_partes = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = 'gestion_arriendos'
    AND table_name = 'usuarios'
    AND index_name = 'uq_usuarios_correo'
);

SET @evohka_correo_indice_compatible = IF(
  @evohka_correo_indice_partes = 1
    AND (
      SELECT COUNT(*)
      FROM information_schema.statistics
      WHERE table_schema = 'gestion_arriendos'
        AND table_name = 'usuarios'
        AND index_name = 'uq_usuarios_correo'
        AND non_unique = 0
        AND seq_in_index = 1
        AND column_name = 'correo'
        AND sub_part IS NULL
    ) = 1,
  1,
  0
);

-- Un indice equivalente con otro nombre requiere revision manual; no se crea
-- un segundo indice redundante de manera automatica.
SET @evohka_correo_indices_equivalentes = (
  SELECT COUNT(*)
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = 'gestion_arriendos'
      AND table_name = 'usuarios'
      AND index_name <> 'PRIMARY'
    GROUP BY index_name
    HAVING COUNT(*) = 1
      AND MAX(non_unique) = 0
      AND MAX(seq_in_index = 1 AND column_name = 'correo'
        AND sub_part IS NULL) = 1
  ) AS indices_correo
);

SET @evohka_correo_estado_indice_valido = IF(
  (@evohka_correo_indice_partes = 0
    AND @evohka_correo_indices_equivalentes = 0)
  OR
  (@evohka_correo_indice_compatible = 1
    AND @evohka_correo_indices_equivalentes = 1),
  1,
  0
);

SET @evohka_correo_check_existe = (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = 'gestion_arriendos'
    AND table_name = 'usuarios'
    AND constraint_name = 'chk_usuarios_correo_normalizado'
    AND constraint_type = 'CHECK'
);

SET @evohka_correo_check_compatible = (
  SELECT COUNT(*)
  FROM information_schema.table_constraints AS tc
  INNER JOIN information_schema.check_constraints AS cc
    ON cc.constraint_schema = tc.constraint_schema
   AND cc.constraint_name = tc.constraint_name
  WHERE tc.constraint_schema = 'gestion_arriendos'
    AND tc.table_name = 'usuarios'
    AND tc.constraint_name = 'chk_usuarios_correo_normalizado'
    AND tc.constraint_type = 'CHECK'
    AND tc.enforced = 'YES'
    AND (
      REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        LOWER(cc.check_clause), '`', ''), ' ', ''), CHAR(9), ''),
        CHAR(10), ''), CHAR(13), '')
        LIKE '%cast(correoasbinary)=cast(lower(trim(correo))asbinary)%'
      OR
      REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        LOWER(cc.check_clause), '`', ''), ' ', ''), CHAR(9), ''),
        CHAR(10), ''), CHAR(13), '')
        LIKE '%cast(correoasbinary)=cast(lcase(trim(correo))asbinary)%'
      OR
      REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        LOWER(cc.check_clause), '`', ''), ' ', ''), CHAR(9), ''),
        CHAR(10), ''), CHAR(13), '')
        LIKE '%cast(correoascharcharsetbinary)=cast(lower(trim(correo))ascharcharsetbinary)%'
      OR
      REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        LOWER(cc.check_clause), '`', ''), ' ', ''), CHAR(9), ''),
        CHAR(10), ''), CHAR(13), '')
        LIKE '%cast(correoascharcharsetbinary)=cast(lcase(trim(correo))ascharcharsetbinary)%'
    )
);

SET @evohka_correo_estado_check_valido = IF(
  @evohka_correo_check_existe = 0
  OR (@evohka_correo_check_existe = 1
    AND @evohka_correo_check_compatible = 1),
  1,
  0
);

SET @evohka_correo_autorizada = IF(
  @evohka_correo_base_correcta = 1
    AND @evohka_correo_tabla_compatible = 1
    AND @evohka_correo_columna_compatible = 1
    AND @evohka_correo_estado_indice_valido = 1
    AND @evohka_correo_estado_check_valido = 1
    AND @evohka_correo_usuarios_esperados >= 0
    AND @evohka_correo_usuarios_esperados =
      @evohka_correo_usuarios_actuales
    AND @evohka_correo_duplicados_esperados = 0
    AND @evohka_correo_duplicados_actuales = 0
    AND @evohka_correo_no_canonicos_esperados = 0
    AND @evohka_correo_no_canonicos_actuales = 0
    AND CONVERT(@evohka_correo_confirmar_base USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'AUTORIZO_GESTION_ARRIENDOS_CORREO_UNICO'
          COLLATE utf8mb4_bin
    AND CONVERT(@evohka_correo_confirmar_respaldo USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'RESPALDO_CORREO_UNICO_VERIFICADO' COLLATE utf8mb4_bin
    AND CONVERT(@evohka_correo_confirmar_ddl USING utf8mb4)
      COLLATE utf8mb4_bin =
        _utf8mb4'APLICAR_UNICIDAD_CORREO_USUARIOS' COLLATE utf8mb4_bin,
  1,
  0
);

-- Guarda obligatoria anterior a cualquier DDL.
SET @evohka_correo_guardar = IF(
  @evohka_correo_autorizada = 1,
  'SELECT ''autorizacion_correo_unico_validada'' AS estado',
  'SELECT EVOHKA_ABORTO_MIGRACION_CORREO_UNICO()'
);
PREPARE evohka_correo_sentencia_guardar FROM @evohka_correo_guardar;
EXECUTE evohka_correo_sentencia_guardar;
DEALLOCATE PREPARE evohka_correo_sentencia_guardar;

SELECT
  'pre_ddl' AS etapa,
  @evohka_correo_base_correcta AS base_correcta,
  @evohka_correo_tabla_compatible AS tabla_compatible,
  @evohka_correo_columna_compatible AS columna_compatible,
  @evohka_correo_usuarios_actuales AS usuarios,
  @evohka_correo_duplicados_actuales AS grupos_duplicados,
  @evohka_correo_no_canonicos_actuales AS correos_no_canonicos,
  @evohka_correo_indice_partes AS indice_existente,
  @evohka_correo_check_existe AS check_existente;

SET @evohka_correo_crear_indice = IF(
  @evohka_correo_indice_partes = 0,
  'ALTER TABLE `gestion_arriendos`.`usuarios`
     ADD UNIQUE KEY `uq_usuarios_correo` (`correo`)',
  'SELECT ''uq_usuarios_correo ya existe y es compatible; DDL omitido''
     AS resultado'
);
PREPARE evohka_correo_sentencia_indice
  FROM @evohka_correo_crear_indice;
EXECUTE evohka_correo_sentencia_indice;
DEALLOCATE PREPARE evohka_correo_sentencia_indice;

-- MySQL 8.4 admite CHECK con LOWER, TRIM y CAST por ser funciones integradas
-- deterministas. CAST(... AS BINARY) evita el operador BINARY obsoleto y hace
-- significativas las mayusculas y los espacios en la comprobacion canonica.
SET @evohka_correo_crear_check = IF(
  @evohka_correo_check_existe = 0,
  'ALTER TABLE `gestion_arriendos`.`usuarios`
     ADD CONSTRAINT `chk_usuarios_correo_normalizado`
     CHECK (
       CAST(`correo` AS BINARY) =
       CAST(LOWER(TRIM(`correo`)) AS BINARY)
     ) ENFORCED',
  'SELECT ''chk_usuarios_correo_normalizado ya existe y es compatible;
     DDL omitido'' AS resultado'
);
PREPARE evohka_correo_sentencia_check
  FROM @evohka_correo_crear_check;
EXECUTE evohka_correo_sentencia_check;
DEALLOCATE PREPARE evohka_correo_sentencia_check;

SET @evohka_correo_indice_final = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = 'gestion_arriendos'
    AND table_name = 'usuarios'
    AND index_name = 'uq_usuarios_correo'
    AND non_unique = 0
    AND seq_in_index = 1
    AND column_name = 'correo'
    AND sub_part IS NULL
);

SET @evohka_correo_check_final = (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = 'gestion_arriendos'
    AND table_name = 'usuarios'
    AND constraint_name = 'chk_usuarios_correo_normalizado'
    AND constraint_type = 'CHECK'
    AND enforced = 'YES'
);

SET @evohka_correo_duplicados_finales = (
  SELECT COUNT(*)
  FROM (
    SELECT LOWER(TRIM(correo)) AS correo_normalizado
    FROM `gestion_arriendos`.`usuarios`
    GROUP BY LOWER(TRIM(correo))
    HAVING COUNT(*) > 1
  ) AS correos_duplicados
);

SET @evohka_correo_no_canonicos_finales = (
  SELECT COUNT(*)
  FROM `gestion_arriendos`.`usuarios`
  WHERE CAST(correo AS BINARY) <>
      CAST(LOWER(TRIM(correo)) AS BINARY)
    OR correo REGEXP '[[:space:]]'
);

SET @evohka_correo_resultado_valido = IF(
  @evohka_correo_indice_final = 1
    AND @evohka_correo_check_final = 1
    AND @evohka_correo_duplicados_finales = 0
    AND @evohka_correo_no_canonicos_finales = 0,
  1,
  0
);

SET @evohka_correo_guardar_resultado = IF(
  @evohka_correo_resultado_valido = 1,
  'SELECT ''migracion_correo_unico_verificada'' AS estado',
  'SELECT EVOHKA_ABORTO_VERIFICACION_CORREO_UNICO()'
);
PREPARE evohka_correo_sentencia_resultado
  FROM @evohka_correo_guardar_resultado;
EXECUTE evohka_correo_sentencia_resultado;
DEALLOCATE PREPARE evohka_correo_sentencia_resultado;

-- Verificacion final agregada. No expone correos ni otros datos personales.
SELECT
  'post_ddl' AS etapa,
  @evohka_correo_indice_final AS indice_unico_compatible,
  @evohka_correo_check_final AS check_canonico_enforced,
  @evohka_correo_duplicados_finales AS grupos_duplicados,
  @evohka_correo_no_canonicos_finales AS correos_no_canonicos,
  @evohka_correo_usuarios_actuales AS usuarios;

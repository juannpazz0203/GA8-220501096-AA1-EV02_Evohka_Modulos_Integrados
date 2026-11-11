const {
  sesionesVersionadasActivas
} = require('./seguridad');

const ERROR_ESQUEMA_SEGURIDAD = 'ESQUEMA_SEGURIDAD_INCOMPATIBLE';

function errorEsquema(mensaje) {
  const error = new Error(mensaje);
  error.code = ERROR_ESQUEMA_SEGURIDAD;
  return error;
}

async function verificarEsquemaSeguridad(conexion) {
  if (!sesionesVersionadasActivas()) return;

  const [columnasSesion] = await conexion.execute(
    `SELECT COUNT(*) AS columnas_compatibles
     FROM information_schema.columns
     WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
         CONVERT(DATABASE() USING utf8mb4) COLLATE utf8mb4_bin
       AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
         _utf8mb4'usuarios' COLLATE utf8mb4_bin
       AND CONVERT(column_name USING utf8mb4) COLLATE utf8mb4_bin =
         _utf8mb4'sesion_version' COLLATE utf8mb4_bin
       AND CONVERT(column_type USING utf8mb4) COLLATE utf8mb4_bin =
         _utf8mb4'int unsigned' COLLATE utf8mb4_bin
       AND CONVERT(is_nullable USING utf8mb4) COLLATE utf8mb4_bin =
         _utf8mb4'NO' COLLATE utf8mb4_bin
       AND CONVERT(COALESCE(column_default, '') USING utf8mb4)
         COLLATE utf8mb4_bin = _utf8mb4'0' COLLATE utf8mb4_bin`
  );

  if (Number(columnasSesion[0]?.columnas_compatibles) !== 1) {
    throw errorEsquema(
      'Las sesiones versionadas están activas, pero usuarios.sesion_version no existe o no es compatible'
    );
  }

  const [tablaRecuperacion] = await conexion.execute(
    `SELECT COUNT(*) AS tablas_compatibles
     FROM information_schema.tables
     WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
         CONVERT(DATABASE() USING utf8mb4) COLLATE utf8mb4_bin
       AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
         _utf8mb4'recuperaciones_contrasena' COLLATE utf8mb4_bin
       AND CONVERT(engine USING utf8mb4) COLLATE utf8mb4_bin =
         _utf8mb4'InnoDB' COLLATE utf8mb4_bin`
  );

  if (Number(tablaRecuperacion[0]?.tablas_compatibles) !== 1) {
    throw errorEsquema(
      'La migración RNF03 de seguridad está incompleta: falta recuperaciones_contrasena con motor InnoDB'
    );
  }

  const [columnasRecuperacion] = await conexion.execute(
    `SELECT COUNT(*) AS columnas_compatibles
     FROM information_schema.columns
     WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
         CONVERT(DATABASE() USING utf8mb4) COLLATE utf8mb4_bin
       AND CONVERT(table_name USING utf8mb4) COLLATE utf8mb4_bin =
         _utf8mb4'recuperaciones_contrasena' COLLATE utf8mb4_bin
       AND (
         (column_name = 'id_recuperacion' AND data_type = 'bigint'
           AND column_type = 'bigint unsigned' AND is_nullable = 'NO')
         OR (column_name = 'id_usuario' AND data_type = 'int'
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
         OR (column_name = 'intentos_fallidos' AND data_type = 'tinyint'
           AND column_type = 'tinyint unsigned' AND is_nullable = 'NO')
         OR (column_name = 'fecha_uso' AND data_type = 'datetime'
           AND datetime_precision = 3 AND is_nullable = 'YES')
         OR (column_name = 'fecha_revocacion' AND data_type = 'datetime'
           AND datetime_precision = 3 AND is_nullable = 'YES')
       )`
  );

  if (Number(columnasRecuperacion[0]?.columnas_compatibles) !== 10) {
    throw errorEsquema(
      'La tabla recuperaciones_contrasena no contiene la estructura RNF03 esperada'
    );
  }
}

module.exports = {
  ERROR_ESQUEMA_SEGURIDAD,
  verificarEsquemaSeguridad
};

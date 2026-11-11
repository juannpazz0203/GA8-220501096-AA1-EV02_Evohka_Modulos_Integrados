const ERROR_ESQUEMA_CREDENCIALES = 'ESQUEMA_CREDENCIALES_INCOMPATIBLE';

async function verificarEsquemaCredenciales(conexion) {
  const [filas] = await conexion.execute(
    `SELECT COUNT(*) AS columnas_compatibles
     FROM information_schema.columns
     WHERE CONVERT(table_schema USING utf8mb4) COLLATE utf8mb4_bin =
         CONVERT(DATABASE() USING utf8mb4) COLLATE utf8mb4_bin
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
         _utf8mb4'legacy_plain' COLLATE utf8mb4_bin`
  );

  if (Number(filas[0]?.columnas_compatibles) !== 1) {
    const error = new Error(
      'El esquema RNF03 es obligatorio: falta usuarios.contrasena_algoritmo o su definición no es compatible'
    );
    error.code = ERROR_ESQUEMA_CREDENCIALES;
    throw error;
  }
}

module.exports = {
  ERROR_ESQUEMA_CREDENCIALES,
  verificarEsquemaCredenciales
};

const mysql = require('mysql2/promise');
const {
  ERROR_ESQUEMA_CREDENCIALES,
  verificarEsquemaCredenciales
} = require('./credenciales');
const {
  ERROR_CONFIGURACION_JWT,
  validarConfiguracionJwt
} = require('./seguridad');
const {
  ERROR_ESQUEMA_SEGURIDAD,
  verificarEsquemaSeguridad
} = require('./esquemaSeguridad');

const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  port: Number(process.env.DB_PORT) || 3306,
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'gestion_arriendos',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});

async function probarConexion() {
  let conexion;

  try {
    validarConfiguracionJwt();
    conexion = await pool.getConnection();
    await verificarEsquemaCredenciales(conexion);
    await verificarEsquemaSeguridad(conexion);
    console.log('Conexión y esquemas de seguridad RNF03 verificados');
  } catch (error) {
    if (
      error.code === ERROR_CONFIGURACION_JWT ||
      error.code === ERROR_ESQUEMA_CREDENCIALES ||
      error.code === ERROR_ESQUEMA_SEGURIDAD
    ) {
      throw error;
    }

    const errorConexion = new Error(
      `No se pudo conectar a MySQL. Verifique XAMPP/Laragon y el archivo .env. Detalle: ${error.message}`
    );
    errorConexion.code = error.code || 'ERROR_CONEXION_MYSQL';
    throw errorConexion;
  } finally {
    if (conexion) conexion.release();
  }
}

module.exports = { pool, probarConexion };

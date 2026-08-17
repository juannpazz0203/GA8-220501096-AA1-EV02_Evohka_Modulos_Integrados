const mysql = require('mysql2/promise');

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
    conexion = await pool.getConnection();
    console.log('Conexión correcta con la base de datos MySQL');
  } catch (error) {
    throw new Error(
      `No se pudo conectar a MySQL. Verifique XAMPP/Laragon y el archivo .env. Detalle: ${error.message}`
    );
  } finally {
    if (conexion) conexion.release();
  }
}

module.exports = { pool, probarConexion };

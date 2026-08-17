const { pool } = require('../config/db');

async function obtenerTodasLasPropiedades() {
  const [filas] = await pool.execute(
    `SELECT id, titulo, ciudad, direccion, precio_mensual, estado, created_at
     FROM propiedades_servicio
     ORDER BY id DESC`
  );

  return filas;
}

async function crearPropiedad(datosPropiedad) {
  const { titulo, ciudad, direccion, precio_mensual } = datosPropiedad;

  // Consulta preparada: los signos ? evitan concatenar datos directamente en el SQL.
  const [resultado] = await pool.execute(
    `INSERT INTO propiedades_servicio
      (titulo, ciudad, direccion, precio_mensual)
     VALUES (?, ?, ?, ?)`,
    [titulo.trim(), ciudad.trim(), direccion.trim(), Number(precio_mensual)]
  );

  return {
    id: resultado.insertId,
    titulo: titulo.trim(),
    ciudad: ciudad.trim(),
    direccion: direccion.trim(),
    precio_mensual: Number(precio_mensual),
    estado: 'disponible'
  };
}

module.exports = {
  obtenerTodasLasPropiedades,
  crearPropiedad
};

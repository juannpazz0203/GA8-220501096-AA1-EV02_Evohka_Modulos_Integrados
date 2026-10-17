const { pool } = require('../config/db');

async function obtenerTodasLasPropiedades() {
  const [filas] = await pool.execute(
    `SELECT
      id_propiedad,
      titulo,
      tipo_inmueble,
      direccion,
      barrio,
      ciudad,
      canon,
      habitaciones,
      banos,
      id_propietario
     FROM propiedades
     ORDER BY id_propiedad DESC`
  );

  return filas;
}

async function crearPropiedad(datosPropiedad) {
  const {
    titulo,
    tipo_inmueble,
    direccion,
    barrio,
    ciudad,
    canon,
    habitaciones,
    banos,
    id_propietario
  } = datosPropiedad;

  const [resultado] = await pool.execute(
    `INSERT INTO propiedades
      (
        titulo,
        tipo_inmueble,
        direccion,
        barrio,
        ciudad,
        canon,
        habitaciones,
        banos,
        id_propietario
      )
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      titulo.trim(),
      tipo_inmueble.trim(),
      direccion.trim(),
      barrio.trim(),
      ciudad.trim(),
      Number(canon),
      Number(habitaciones),
      Number(banos),
      Number(id_propietario)
    ]
  );

  return {
    id_propiedad: resultado.insertId,
    titulo: titulo.trim(),
    tipo_inmueble: tipo_inmueble.trim(),
    direccion: direccion.trim(),
    barrio: barrio.trim(),
    ciudad: ciudad.trim(),
    canon: Number(canon),
    habitaciones: Number(habitaciones),
    banos: Number(banos),
    id_propietario: Number(id_propietario)
  };
}

module.exports = {
  obtenerTodasLasPropiedades,
  crearPropiedad
};
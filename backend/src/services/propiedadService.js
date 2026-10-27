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

async function obtenerPropiedadPorId(idPropiedad) {
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
     WHERE id_propiedad = ?
     LIMIT 1`,
    [Number(idPropiedad)]
  );

  return filas[0] || null;
}

async function obtenerPropiedadDelPropietario(
  idPropiedad,
  idPropietario
) {
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
     WHERE id_propiedad = ?
       AND id_propietario = ?
     LIMIT 1`,
    [
      Number(idPropiedad),
      Number(idPropietario)
    ]
  );

  return filas[0] || null;
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

async function actualizarPropiedad(
  idPropiedad,
  idPropietario,
  datosPropiedad
) {
  const {
    titulo,
    tipo_inmueble,
    direccion,
    barrio,
    ciudad,
    canon,
    habitaciones,
    banos
  } = datosPropiedad;

  await pool.execute(
    `UPDATE propiedades
     SET
       titulo = ?,
       tipo_inmueble = ?,
       direccion = ?,
       barrio = ?,
       ciudad = ?,
       canon = ?,
       habitaciones = ?,
       banos = ?
     WHERE id_propiedad = ?
       AND id_propietario = ?`,
    [
      titulo.trim(),
      tipo_inmueble.trim(),
      direccion.trim(),
      barrio.trim(),
      ciudad.trim(),
      Number(canon),
      Number(habitaciones),
      Number(banos),
      Number(idPropiedad),
      Number(idPropietario)
    ]
  );

  return obtenerPropiedadDelPropietario(
    idPropiedad,
    idPropietario
  );
}

async function eliminarPropiedad(
  idPropiedad,
  idPropietario
) {
  const [resultado] = await pool.execute(
    `DELETE FROM propiedades
     WHERE id_propiedad = ?
       AND id_propietario = ?`,
    [
      Number(idPropiedad),
      Number(idPropietario)
    ]
  );

  return resultado.affectedRows > 0;
}

module.exports = {
  obtenerTodasLasPropiedades,
  obtenerPropiedadPorId,
  crearPropiedad,
  actualizarPropiedad,
  eliminarPropiedad
};

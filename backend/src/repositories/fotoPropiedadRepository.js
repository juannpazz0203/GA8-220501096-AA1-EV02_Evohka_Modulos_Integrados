const { pool } = require('../config/db');

function crearOperaciones(ejecutor) {
  async function obtenerPropiedad(idPropiedad, bloquear = false) {
    const [filas] = await ejecutor.execute(
      `SELECT id_propiedad, id_propietario
       FROM propiedades
       WHERE id_propiedad = ?
       LIMIT 1${bloquear ? ' FOR UPDATE' : ''}`,
      [Number(idPropiedad)]
    );

    return filas[0] || null;
  }

  async function contarFotografias(idPropiedad) {
    const [filas] = await ejecutor.execute(
      `SELECT COUNT(*) AS total
       FROM propiedad_fotos
       WHERE id_propiedad = ?`,
      [Number(idPropiedad)]
    );

    return Number(filas[0]?.total || 0);
  }

  async function obtenerMayorOrden(idPropiedad) {
    const [filas] = await ejecutor.execute(
      `SELECT COALESCE(MAX(orden), -1) AS mayor_orden
       FROM propiedad_fotos
       WHERE id_propiedad = ?`,
      [Number(idPropiedad)]
    );

    return Number(filas[0]?.mayor_orden ?? -1);
  }

  async function insertarFotografia(datos) {
    const [resultado] = await ejecutor.execute(
      `INSERT INTO propiedad_fotos
        (
          id_propiedad,
          ruta_relativa,
          mime_type,
          tamano_bytes,
          orden
        )
       VALUES (?, ?, ?, ?, ?)`,
      [
        Number(datos.id_propiedad),
        datos.ruta_relativa,
        datos.mime_type,
        Number(datos.tamano_bytes),
        Number(datos.orden)
      ]
    );

    return Number(resultado.insertId);
  }

  async function listarFotografias(idPropiedad, bloquear = false) {
    const [filas] = await ejecutor.execute(
      `SELECT
        id_foto,
        id_propiedad,
        ruta_relativa,
        mime_type,
        tamano_bytes,
        orden,
        fecha_creacion
       FROM propiedad_fotos
       WHERE id_propiedad = ?
       ORDER BY orden ASC, id_foto ASC${bloquear ? ' FOR UPDATE' : ''}`,
      [Number(idPropiedad)]
    );

    return filas;
  }

  async function obtenerFotografia(idPropiedad, idFoto, bloquear = false) {
    const [filas] = await ejecutor.execute(
      `SELECT
        id_foto,
        id_propiedad,
        ruta_relativa,
        mime_type,
        tamano_bytes,
        orden,
        fecha_creacion
       FROM propiedad_fotos
       WHERE id_propiedad = ?
         AND id_foto = ?
       LIMIT 1${bloquear ? ' FOR UPDATE' : ''}`,
      [Number(idPropiedad), Number(idFoto)]
    );

    return filas[0] || null;
  }

  async function eliminarFotografia(idPropiedad, idFoto) {
    const [resultado] = await ejecutor.execute(
      `DELETE FROM propiedad_fotos
       WHERE id_propiedad = ?
         AND id_foto = ?`,
      [Number(idPropiedad), Number(idFoto)]
    );

    return resultado.affectedRows > 0;
  }

  async function desplazarOrdenTemporal(idPropiedad) {
    await ejecutor.execute(
      `UPDATE propiedad_fotos
       SET orden = orden + 100
       WHERE id_propiedad = ?`,
      [Number(idPropiedad)]
    );
  }

  async function actualizarOrden(idPropiedad, idFoto, orden) {
    const [resultado] = await ejecutor.execute(
      `UPDATE propiedad_fotos
       SET orden = ?
       WHERE id_propiedad = ?
         AND id_foto = ?`,
      [Number(orden), Number(idPropiedad), Number(idFoto)]
    );

    return resultado.affectedRows > 0;
  }

  return {
    obtenerPropiedad,
    contarFotografias,
    obtenerMayorOrden,
    insertarFotografia,
    listarFotografias,
    obtenerFotografia,
    eliminarFotografia,
    desplazarOrdenTemporal,
    actualizarOrden
  };
}

function crearFotoPropiedadRepository(poolDb = pool) {
  const operacionesPool = crearOperaciones(poolDb);

  async function conTransaccion(operacion) {
    const conexion = await poolDb.getConnection();

    try {
      await conexion.beginTransaction();
      const resultado = await operacion(crearOperaciones(conexion));
      await conexion.commit();
      return resultado;
    } catch (error) {
      await conexion.rollback();
      throw error;
    } finally {
      conexion.release();
    }
  }

  return {
    ...operacionesPool,
    conTransaccion
  };
}

const fotoPropiedadRepository = crearFotoPropiedadRepository();

module.exports = {
  crearFotoPropiedadRepository,
  fotoPropiedadRepository
};

const { pool } = require('../config/db');
const notificacionService = require('./notificacionService');

const SELECCION_FALLA = `SELECT
  r.id_reporte AS id_falla,
  r.id_usuario,
  u.nombre AS nombre_arrendatario,
  u.apellido AS apellido_arrendatario,
  r.id_propiedad,
  p.titulo AS titulo_propiedad,
  p.id_propietario,
  r.categoria,
  r.descripcion,
  r.fecha_reporte,
  r.estado
 FROM reportes AS r
 INNER JOIN usuarios AS u ON u.id_usuario = r.id_usuario
 INNER JOIN propiedades AS p ON p.id_propiedad = r.id_propiedad`;

async function obtenerFalla(idFalla, ejecutor = pool) {
  const [filas] = await ejecutor.execute(
    `${SELECCION_FALLA} WHERE r.id_reporte = ? LIMIT 1`,
    [Number(idFalla)]
  );
  return filas[0] || null;
}

async function listarFallas(idUsuario, rol) {
  const condicion = rol === 'propietario'
    ? 'p.id_propietario = ?'
    : 'r.id_usuario = ?';
  const [filas] = await pool.execute(
    `${SELECCION_FALLA}
     WHERE ${condicion}
     ORDER BY
       CASE LOWER(r.estado)
         WHEN 'pendiente' THEN 0
         WHEN 'en_proceso' THEN 1
         ELSE 2
       END,
       r.fecha_reporte DESC,
       r.id_reporte DESC`,
    [Number(idUsuario)]
  );
  return filas;
}

async function listarPropiedadesHabilitadas(idArrendatario) {
  const [filas] = await pool.execute(
    `SELECT DISTINCT
      p.id_propiedad,
      p.titulo,
      p.direccion,
      p.ciudad
     FROM contratos AS c
     INNER JOIN solicitudes AS s ON s.id_solicitud = c.id_solicitud
     INNER JOIN propiedades AS p ON p.id_propiedad = s.id_propiedad
     WHERE s.id_usuario = ?
       AND LOWER(c.estado) = 'activo'
     ORDER BY p.titulo ASC`,
    [Number(idArrendatario)]
  );
  return filas;
}

async function crearFalla(datos) {
  const conexion = await pool.getConnection();
  try {
    await conexion.beginTransaction();
    const [relaciones] = await conexion.execute(
      `SELECT p.id_propietario, p.titulo
       FROM contratos AS c
       INNER JOIN solicitudes AS s ON s.id_solicitud = c.id_solicitud
       INNER JOIN propiedades AS p ON p.id_propiedad = s.id_propiedad
       WHERE s.id_usuario = ?
         AND p.id_propiedad = ?
         AND LOWER(c.estado) = 'activo'
       LIMIT 1
       FOR UPDATE`,
      [Number(datos.id_usuario), Number(datos.id_propiedad)]
    );

    const relacion = relaciones[0];
    if (!relacion) {
      await conexion.rollback();
      return { error: 'sin_relacion_activa' };
    }

    const [resultado] = await conexion.execute(
      `INSERT INTO reportes
        (id_usuario, id_propiedad, categoria, descripcion, fecha_reporte, estado)
       VALUES (?, ?, ?, ?, CURDATE(), 'pendiente')`,
      [
        Number(datos.id_usuario),
        Number(datos.id_propiedad),
        datos.categoria,
        datos.descripcion
      ]
    );

    await notificacionService.crearNotificacion(
      conexion,
      relacion.id_propietario,
      'Nueva falla reportada',
      `Se reportó una falla en ${relacion.titulo}.`
    );

    const falla = await obtenerFalla(resultado.insertId, conexion);
    await conexion.commit();
    return { datos: falla };
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

async function actualizarEstadoFalla(idFalla, idPropietario, estado) {
  const conexion = await pool.getConnection();
  try {
    await conexion.beginTransaction();
    const [filas] = await conexion.execute(
      `SELECT r.id_usuario, r.estado, p.id_propietario, p.titulo
       FROM reportes AS r
       INNER JOIN propiedades AS p ON p.id_propiedad = r.id_propiedad
       WHERE r.id_reporte = ?
       LIMIT 1
       FOR UPDATE`,
      [Number(idFalla)]
    );
    const falla = filas[0];
    if (!falla) {
      await conexion.rollback();
      return { error: 'no_existe' };
    }
    if (Number(falla.id_propietario) !== Number(idPropietario)) {
      await conexion.rollback();
      return { error: 'sin_permiso' };
    }
    if (String(falla.estado).toLowerCase() === 'resuelta' && estado !== 'resuelta') {
      await conexion.rollback();
      return { error: 'ya_resuelta' };
    }

    await conexion.execute(
      'UPDATE reportes SET estado = ? WHERE id_reporte = ?',
      [estado, Number(idFalla)]
    );
    await notificacionService.crearNotificacion(
      conexion,
      falla.id_usuario,
      'Falla actualizada',
      `La falla de ${falla.titulo} cambió a ${estado.replace('_', ' ')}.`
    );
    const actualizada = await obtenerFalla(idFalla, conexion);
    await conexion.commit();
    return { datos: actualizada };
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

module.exports = {
  listarFallas,
  listarPropiedadesHabilitadas,
  crearFalla,
  actualizarEstadoFalla
};

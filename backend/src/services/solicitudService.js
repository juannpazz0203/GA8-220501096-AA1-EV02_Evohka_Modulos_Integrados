const { pool } = require('../config/db');

async function obtenerSolicitudPorId(idSolicitud) {
  const [filas] = await pool.execute(
    `SELECT
      s.id_solicitud,
      s.id_usuario,
      u.nombre AS nombre_solicitante,
      u.apellido AS apellido_solicitante,
      s.id_propiedad,
      p.titulo AS titulo_propiedad,
      p.id_propietario,
      s.fecha_solicitud,
      s.estado,
      s.observaciones
     FROM solicitudes AS s
     INNER JOIN usuarios AS u
       ON u.id_usuario = s.id_usuario
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     WHERE s.id_solicitud = ?
     LIMIT 1`,
    [Number(idSolicitud)]
  );

  return filas[0] || null;
}

async function buscarSolicitudPendiente(
  idUsuario,
  idPropiedad
) {
  const [filas] = await pool.execute(
    `SELECT id_solicitud
     FROM solicitudes
     WHERE id_usuario = ?
       AND id_propiedad = ?
       AND estado = 'pendiente'
     LIMIT 1`,
    [
      Number(idUsuario),
      Number(idPropiedad)
    ]
  );

  return filas[0] || null;
}

async function crearSolicitud(
  idUsuario,
  idPropiedad
) {
  const [resultado] = await pool.execute(
    `INSERT INTO solicitudes
      (
        id_usuario,
        id_propiedad,
        fecha_solicitud,
        estado,
        observaciones
      )
     VALUES (?, ?, CURDATE(), 'pendiente', NULL)`,
    [
      Number(idUsuario),
      Number(idPropiedad)
    ]
  );

  return obtenerSolicitudPorId(
    resultado.insertId
  );
}

async function listarSolicitudesRecibidas(
  idPropietario
) {
  const [filas] = await pool.execute(
    `SELECT
      s.id_solicitud,
      s.id_usuario,
      u.nombre AS nombre_solicitante,
      u.apellido AS apellido_solicitante,
      s.id_propiedad,
      p.titulo AS titulo_propiedad,
      p.id_propietario,
      s.fecha_solicitud,
      s.estado,
      s.observaciones
     FROM solicitudes AS s
     INNER JOIN usuarios AS u
       ON u.id_usuario = s.id_usuario
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     WHERE p.id_propietario = ?
     ORDER BY
       CASE WHEN s.estado = 'pendiente' THEN 0 ELSE 1 END,
       s.fecha_solicitud DESC,
       s.id_solicitud DESC`,
    [Number(idPropietario)]
  );

  return filas;
}

async function actualizarEstadoSolicitud(
  idSolicitud,
  idPropietario,
  estado
) {
  const [resultado] = await pool.execute(
    `UPDATE solicitudes AS s
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     SET s.estado = ?
     WHERE s.id_solicitud = ?
       AND p.id_propietario = ?
       AND s.estado = 'pendiente'`,
    [
      estado,
      Number(idSolicitud),
      Number(idPropietario)
    ]
  );

  if (resultado.affectedRows === 0) {
    return null;
  }

  return obtenerSolicitudPorId(idSolicitud);
}

module.exports = {
  obtenerSolicitudPorId,
  buscarSolicitudPendiente,
  crearSolicitud,
  listarSolicitudesRecibidas,
  actualizarEstadoSolicitud
};

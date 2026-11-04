const { pool } = require('../config/db');

function normalizarNotificacion(fila) {
  if (!fila) {
    return null;
  }

  return {
    ...fila,
    leida: Number(fila.leida) === 1
  };
}

async function crearNotificacion(
  ejecutor,
  idUsuario,
  titulo,
  mensaje
) {
  await ejecutor.execute(
    `INSERT INTO notificaciones
      (
        id_usuario,
        titulo,
        mensaje,
        fecha,
        leida
      )
     VALUES (?, ?, ?, NOW(), 0)`,
    [
      Number(idUsuario),
      titulo,
      mensaje
    ]
  );
}

async function listarNotificaciones(idUsuario) {
  const [filas] = await pool.execute(
    `SELECT
      id_notificacion,
      id_usuario,
      titulo,
      mensaje,
      fecha,
      leida
     FROM notificaciones
     WHERE id_usuario = ?
     ORDER BY leida ASC, fecha DESC, id_notificacion DESC`,
    [Number(idUsuario)]
  );

  return filas.map(normalizarNotificacion);
}

async function marcarComoLeida(
  idNotificacion,
  idUsuario
) {
  const [resultado] = await pool.execute(
    `UPDATE notificaciones
     SET leida = 1
     WHERE id_notificacion = ?
       AND id_usuario = ?`,
    [
      Number(idNotificacion),
      Number(idUsuario)
    ]
  );

  if (resultado.affectedRows === 0) {
    return null;
  }

  const [filas] = await pool.execute(
    `SELECT
      id_notificacion,
      id_usuario,
      titulo,
      mensaje,
      fecha,
      leida
     FROM notificaciones
     WHERE id_notificacion = ?
       AND id_usuario = ?
     LIMIT 1`,
    [
      Number(idNotificacion),
      Number(idUsuario)
    ]
  );

  return normalizarNotificacion(
    filas[0] || null
  );
}

module.exports = {
  crearNotificacion,
  listarNotificaciones,
  marcarComoLeida
};

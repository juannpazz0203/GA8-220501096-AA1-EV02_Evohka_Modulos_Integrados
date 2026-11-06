const { pool } = require('../config/db');
const crypto = require('node:crypto');

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

async function crearNotificacionSiNoExiste(
  ejecutor,
  idUsuario,
  titulo,
  mensaje
) {
  const huella = crypto
    .createHash('sha256')
    .update(`${Number(idUsuario)}|${titulo}|${mensaje}`)
    .digest('hex');
  const nombreBloqueo = `evohka:${huella.slice(0, 55)}`;

  const [bloqueos] = await ejecutor.execute(
    'SELECT GET_LOCK(?, 5) AS adquirido',
    [nombreBloqueo]
  );

  if (Number(bloqueos[0]?.adquirido) !== 1) {
    throw new Error('No fue posible bloquear la creación del recordatorio');
  }

  try {
    const [existentes] = await ejecutor.execute(
      `SELECT id_notificacion
       FROM notificaciones
       WHERE id_usuario = ?
         AND titulo = ?
         AND mensaje = ?
       LIMIT 1`,
      [Number(idUsuario), titulo, mensaje]
    );

    if (existentes.length > 0) return false;

    await crearNotificacion(
      ejecutor,
      idUsuario,
      titulo,
      mensaje
    );
    return true;
  } finally {
    await ejecutor.execute(
      'SELECT RELEASE_LOCK(?) AS liberado',
      [nombreBloqueo]
    );
  }
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
  crearNotificacionSiNoExiste,
  listarNotificaciones,
  marcarComoLeida
};

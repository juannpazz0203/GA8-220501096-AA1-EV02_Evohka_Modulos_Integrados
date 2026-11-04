const { pool } = require('../config/db');
const notificacionService = require('./notificacionService');

function normalizarMensaje(fila) {
  return fila ? { ...fila, leido: Number(fila.leido) === 1 } : null;
}

async function relacionValida(idUsuario, idOtroUsuario, idPropiedad, ejecutor = pool) {
  const [filas] = await ejecutor.execute(
    `SELECT p.titulo
     FROM contratos AS c
     INNER JOIN solicitudes AS s ON s.id_solicitud = c.id_solicitud
     INNER JOIN propiedades AS p ON p.id_propiedad = s.id_propiedad
     WHERE p.id_propiedad = ?
       AND (
         (p.id_propietario = ? AND s.id_usuario = ?)
         OR
         (p.id_propietario = ? AND s.id_usuario = ?)
       )
     LIMIT 1`,
    [
      Number(idPropiedad),
      Number(idUsuario),
      Number(idOtroUsuario),
      Number(idOtroUsuario),
      Number(idUsuario)
    ]
  );
  return filas[0] || null;
}

async function listarConversaciones(idUsuario, rol) {
  const esPropietario = rol === 'propietario';
  const otroId = esPropietario ? 's.id_usuario' : 'p.id_propietario';
  const otroJoin = esPropietario ? 'u.id_usuario = s.id_usuario' : 'u.id_usuario = p.id_propietario';
  const condicion = esPropietario ? 'p.id_propietario = ?' : 's.id_usuario = ?';

  const [filas] = await pool.execute(
    `SELECT
      p.id_propiedad,
      p.titulo AS titulo_propiedad,
      ${otroId} AS id_otro_usuario,
      u.nombre AS nombre_otro_usuario,
      u.apellido AS apellido_otro_usuario,
      (
        SELECT m.contenido
        FROM mensajes AS m
        WHERE m.id_propiedad = p.id_propiedad
          AND ((m.id_remitente = ? AND m.id_destinatario = ${otroId})
            OR (m.id_remitente = ${otroId} AND m.id_destinatario = ?))
        ORDER BY m.fecha_envio DESC, m.id_mensaje DESC
        LIMIT 1
      ) AS ultimo_mensaje,
      (
        SELECT m.fecha_envio
        FROM mensajes AS m
        WHERE m.id_propiedad = p.id_propiedad
          AND ((m.id_remitente = ? AND m.id_destinatario = ${otroId})
            OR (m.id_remitente = ${otroId} AND m.id_destinatario = ?))
        ORDER BY m.fecha_envio DESC, m.id_mensaje DESC
        LIMIT 1
      ) AS fecha_ultimo_mensaje,
      (
        SELECT COUNT(*)
        FROM mensajes AS m
        WHERE m.id_propiedad = p.id_propiedad
          AND m.id_remitente = ${otroId}
          AND m.id_destinatario = ?
          AND m.leido = 0
      ) AS no_leidos
     FROM contratos AS c
     INNER JOIN solicitudes AS s ON s.id_solicitud = c.id_solicitud
     INNER JOIN propiedades AS p ON p.id_propiedad = s.id_propiedad
     INNER JOIN usuarios AS u ON ${otroJoin}
     WHERE ${condicion}
     GROUP BY p.id_propiedad, p.titulo, ${otroId}, u.nombre, u.apellido
     ORDER BY fecha_ultimo_mensaje IS NULL, fecha_ultimo_mensaje DESC, p.titulo ASC`,
    [
      Number(idUsuario),
      Number(idUsuario),
      Number(idUsuario),
      Number(idUsuario),
      Number(idUsuario),
      Number(idUsuario)
    ]
  );
  return filas.map((fila) => ({ ...fila, no_leidos: Number(fila.no_leidos || 0) }));
}

async function obtenerMensaje(idMensaje, ejecutor = pool) {
  const [filas] = await ejecutor.execute(
    `SELECT id_mensaje, id_remitente, id_destinatario, id_propiedad,
      contenido, fecha_envio, leido
     FROM mensajes
     WHERE id_mensaje = ? LIMIT 1`,
    [Number(idMensaje)]
  );
  return normalizarMensaje(filas[0] || null);
}

async function listarMensajes(idUsuario, idOtroUsuario, idPropiedad) {
  if (!await relacionValida(idUsuario, idOtroUsuario, idPropiedad)) {
    return { error: 'sin_permiso' };
  }
  const [filas] = await pool.execute(
    `SELECT id_mensaje, id_remitente, id_destinatario, id_propiedad,
      contenido, fecha_envio, leido
     FROM mensajes
     WHERE id_propiedad = ?
       AND ((id_remitente = ? AND id_destinatario = ?)
         OR (id_remitente = ? AND id_destinatario = ?))
     ORDER BY fecha_envio ASC, id_mensaje ASC`,
    [
      Number(idPropiedad),
      Number(idUsuario),
      Number(idOtroUsuario),
      Number(idOtroUsuario),
      Number(idUsuario)
    ]
  );
  return { datos: filas.map(normalizarMensaje) };
}

async function enviarMensaje(datos) {
  const conexion = await pool.getConnection();
  try {
    await conexion.beginTransaction();
    const relacion = await relacionValida(
      datos.id_remitente,
      datos.id_destinatario,
      datos.id_propiedad,
      conexion
    );
    if (!relacion) {
      await conexion.rollback();
      return { error: 'sin_relacion' };
    }
    const [resultado] = await conexion.execute(
      `INSERT INTO mensajes
        (id_remitente, id_destinatario, id_propiedad, contenido, fecha_envio, leido)
       VALUES (?, ?, ?, ?, NOW(), 0)`,
      [
        Number(datos.id_remitente),
        Number(datos.id_destinatario),
        Number(datos.id_propiedad),
        datos.contenido
      ]
    );
    await notificacionService.crearNotificacion(
      conexion,
      datos.id_destinatario,
      'Nuevo mensaje',
      `Recibiste un mensaje sobre ${relacion.titulo}.`
    );
    const mensaje = await obtenerMensaje(resultado.insertId, conexion);
    await conexion.commit();
    return { datos: mensaje };
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

async function marcarLeidos(idUsuario, idOtroUsuario, idPropiedad) {
  if (!await relacionValida(idUsuario, idOtroUsuario, idPropiedad)) {
    return { error: 'sin_permiso' };
  }
  const [resultado] = await pool.execute(
    `UPDATE mensajes
     SET leido = 1
     WHERE id_propiedad = ?
       AND id_remitente = ?
       AND id_destinatario = ?
       AND leido = 0`,
    [Number(idPropiedad), Number(idOtroUsuario), Number(idUsuario)]
  );
  return { actualizados: resultado.affectedRows };
}

module.exports = {
  listarConversaciones,
  listarMensajes,
  enviarMensaje,
  marcarLeidos
};

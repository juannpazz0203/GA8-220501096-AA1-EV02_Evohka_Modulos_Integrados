const notificacionService = require('../services/notificacionService');

function obtenerId(valor) {
  const id = Number(valor);

  return Number.isInteger(id) && id > 0
    ? id
    : null;
}

async function listarNotificaciones(req, res) {
  try {
    const notificaciones =
      await notificacionService.listarNotificaciones(
        req.usuario.id_usuario
      );

    return res.status(200).json({
      mensaje: 'Notificaciones recuperadas correctamente',
      total: notificaciones.length,
      no_leidas: notificaciones.filter(
        (notificacion) => !notificacion.leida
      ).length,
      datos: notificaciones
    });
  } catch (error) {
    console.error(
      'Error al listar notificaciones:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible consultar las notificaciones'
    });
  }
}

async function marcarComoLeida(req, res) {
  try {
    const idNotificacion = obtenerId(
      req.params.id
    );

    if (!idNotificacion) {
      return res.status(400).json({
        mensaje: 'El identificador de la notificación no es válido'
      });
    }

    const notificacion =
      await notificacionService.marcarComoLeida(
        idNotificacion,
        req.usuario.id_usuario
      );

    if (!notificacion) {
      return res.status(404).json({
        mensaje: 'La notificación no existe o no pertenece al usuario autenticado'
      });
    }

    return res.status(200).json({
      mensaje: 'Notificación marcada como leída',
      datos: notificacion
    });
  } catch (error) {
    console.error(
      'Error al actualizar notificación:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible actualizar la notificación'
    });
  }
}

module.exports = {
  listarNotificaciones,
  marcarComoLeida
};

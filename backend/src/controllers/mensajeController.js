const mensajeService = require('../services/mensajeService');

function idValido(valor) {
  const id = Number(valor);
  return Number.isInteger(id) && id > 0 ? id : null;
}

function validarRol(req, res) {
  if (!['arrendatario', 'propietario'].includes(req.usuario?.rol)) {
    res.status(403).json({ mensaje: 'El rol autenticado no puede usar la mensajería' });
    return false;
  }
  return true;
}

async function listarConversaciones(req, res) {
  try {
    if (!validarRol(req, res)) return;
    const datos = await mensajeService.listarConversaciones(
      req.usuario.id_usuario,
      req.usuario.rol
    );
    return res.status(200).json({
      mensaje: 'Conversaciones recuperadas correctamente',
      total: datos.length,
      datos
    });
  } catch (error) {
    console.error('Error al listar conversaciones:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible consultar las conversaciones' });
  }
}

async function listarMensajes(req, res) {
  try {
    if (!validarRol(req, res)) return;
    const idPropiedad = idValido(req.query.propiedad);
    const idOtroUsuario = idValido(req.query.usuario);
    if (!idPropiedad || !idOtroUsuario) {
      return res.status(400).json({ mensaje: 'La propiedad y el usuario son obligatorios' });
    }
    const resultado = await mensajeService.listarMensajes(
      req.usuario.id_usuario,
      idOtroUsuario,
      idPropiedad
    );
    if (resultado.error) {
      return res.status(403).json({ mensaje: 'No tiene permiso para consultar esta conversación' });
    }
    return res.status(200).json({
      mensaje: 'Mensajes recuperados correctamente',
      total: resultado.datos.length,
      datos: resultado.datos
    });
  } catch (error) {
    console.error('Error al listar mensajes:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible consultar los mensajes' });
  }
}

async function enviarMensaje(req, res) {
  try {
    if (!validarRol(req, res)) return;
    const idDestinatario = idValido(req.body?.id_destinatario);
    const idPropiedad = idValido(req.body?.id_propiedad);
    const contenido = typeof req.body?.contenido === 'string'
      ? req.body.contenido.trim()
      : '';
    if (!idDestinatario || !idPropiedad || !contenido || contenido.length > 255) {
      return res.status(400).json({
        mensaje: 'El destinatario, la propiedad y un mensaje de máximo 255 caracteres son obligatorios'
      });
    }
    if (idDestinatario === Number(req.usuario.id_usuario)) {
      return res.status(400).json({ mensaje: 'No puede enviarse mensajes a sí mismo' });
    }

    const resultado = await mensajeService.enviarMensaje({
      id_remitente: req.usuario.id_usuario,
      id_destinatario: idDestinatario,
      id_propiedad: idPropiedad,
      contenido
    });
    if (resultado.error) {
      return res.status(403).json({ mensaje: 'No existe una relación de arriendo válida para enviar el mensaje' });
    }
    return res.status(201).json({
      mensaje: 'Mensaje enviado correctamente',
      datos: resultado.datos
    });
  } catch (error) {
    console.error('Error al enviar mensaje:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible enviar el mensaje' });
  }
}

async function marcarLeidos(req, res) {
  try {
    if (!validarRol(req, res)) return;
    const idPropiedad = idValido(req.body?.id_propiedad);
    const idOtroUsuario = idValido(req.body?.id_usuario);
    if (!idPropiedad || !idOtroUsuario) {
      return res.status(400).json({ mensaje: 'La propiedad y el usuario son obligatorios' });
    }
    const resultado = await mensajeService.marcarLeidos(
      req.usuario.id_usuario,
      idOtroUsuario,
      idPropiedad
    );
    if (resultado.error) {
      return res.status(403).json({ mensaje: 'No tiene permiso para modificar esta conversación' });
    }
    return res.status(200).json({
      mensaje: 'Mensajes marcados como leídos',
      actualizados: resultado.actualizados
    });
  } catch (error) {
    console.error('Error al marcar mensajes:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible actualizar los mensajes' });
  }
}

module.exports = {
  listarConversaciones,
  listarMensajes,
  enviarMensaje,
  marcarLeidos
};

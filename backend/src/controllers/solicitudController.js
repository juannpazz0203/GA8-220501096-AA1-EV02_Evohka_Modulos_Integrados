const propiedadService = require('../services/propiedadService');
const solicitudService = require('../services/solicitudService');

const ESTADO_PENDIENTE = 'pendiente';
const ESTADOS_RESPUESTA = [
  'aprobada',
  'rechazada'
];

function obtenerId(valor) {
  const id = Number(valor);

  if (!Number.isInteger(id) || id <= 0) {
    return null;
  }

  return id;
}

async function crearSolicitud(req, res) {
  try {
    if (req.usuario?.rol !== 'arrendatario') {
      return res.status(403).json({
        mensaje: 'Solo los arrendatarios pueden solicitar un arriendo'
      });
    }

    const idPropiedad = obtenerId(
      req.body?.id_propiedad
    );

    if (!idPropiedad) {
      return res.status(400).json({
        mensaje: 'El identificador de la propiedad no es válido'
      });
    }

    const propiedad =
      await propiedadService.obtenerPropiedadPorId(
        idPropiedad
      );

    if (!propiedad) {
      return res.status(404).json({
        mensaje: 'La propiedad no existe'
      });
    }

    const solicitudPendiente =
      await solicitudService.buscarSolicitudPendiente(
        req.usuario.id_usuario,
        idPropiedad
      );

    if (solicitudPendiente) {
      return res.status(409).json({
        mensaje: 'Ya tiene una solicitud pendiente para esta propiedad'
      });
    }

    const solicitud =
      await solicitudService.crearSolicitud(
        req.usuario.id_usuario,
        idPropiedad
      );

    return res.status(201).json({
      mensaje: 'Solicitud de arriendo registrada correctamente',
      datos: solicitud
    });
  } catch (error) {
    console.error(
      'Error al crear solicitud de arriendo:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible registrar la solicitud de arriendo'
    });
  }
}

async function listarSolicitudesRecibidas(req, res) {
  try {
    if (req.usuario?.rol !== 'propietario') {
      return res.status(403).json({
        mensaje: 'Solo los propietarios pueden consultar solicitudes recibidas'
      });
    }

    const solicitudes =
      await solicitudService.listarSolicitudesRecibidas(
        req.usuario.id_usuario
      );

    return res.status(200).json({
      mensaje: 'Solicitudes recibidas recuperadas correctamente',
      total: solicitudes.length,
      datos: solicitudes
    });
  } catch (error) {
    console.error(
      'Error al listar solicitudes recibidas:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible consultar las solicitudes recibidas'
    });
  }
}

async function actualizarEstadoSolicitud(req, res) {
  try {
    if (req.usuario?.rol !== 'propietario') {
      return res.status(403).json({
        mensaje: 'Solo los propietarios pueden responder solicitudes'
      });
    }

    const idSolicitud = obtenerId(
      req.params.id
    );

    if (!idSolicitud) {
      return res.status(400).json({
        mensaje: 'El identificador de la solicitud no es válido'
      });
    }

    const estado =
      typeof req.body?.estado === 'string'
        ? req.body.estado.trim().toLowerCase()
        : '';

    if (!ESTADOS_RESPUESTA.includes(estado)) {
      return res.status(400).json({
        mensaje: 'El estado debe ser aprobada o rechazada'
      });
    }

    const solicitud =
      await solicitudService.obtenerSolicitudPorId(
        idSolicitud
      );

    if (!solicitud) {
      return res.status(404).json({
        mensaje: 'La solicitud no existe'
      });
    }

    if (
      Number(solicitud.id_propietario) !==
      Number(req.usuario.id_usuario)
    ) {
      return res.status(403).json({
        mensaje: 'No tiene permiso para responder esta solicitud'
      });
    }

    if (solicitud.estado !== ESTADO_PENDIENTE) {
      return res.status(409).json({
        mensaje: 'Solo se pueden responder solicitudes pendientes'
      });
    }

    const solicitudActualizada =
      await solicitudService.actualizarEstadoSolicitud(
        idSolicitud,
        req.usuario.id_usuario,
        estado
      );

    if (!solicitudActualizada) {
      return res.status(409).json({
        mensaje: 'La solicitud ya no se encuentra pendiente'
      });
    }

    return res.status(200).json({
      mensaje:
        estado === 'aprobada'
          ? 'Solicitud aprobada correctamente'
          : 'Solicitud rechazada correctamente',
      datos: solicitudActualizada
    });
  } catch (error) {
    console.error(
      'Error al actualizar solicitud:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible actualizar la solicitud'
    });
  }
}

module.exports = {
  crearSolicitud,
  listarSolicitudesRecibidas,
  actualizarEstadoSolicitud
};

const fotoPropiedadService = require('../services/fotoPropiedadService');
const { ErrorFotoPropiedad } = require('../services/fotoPropiedadErrores');

function exigirPropietario(req, res, next) {
  if (req.usuario?.rol !== 'propietario') {
    return res.status(403).json({
      mensaje: 'Solo el propietario puede administrar fotografías'
    });
  }

  return next();
}

function responderError(error, res, operacion) {
  if (error instanceof ErrorFotoPropiedad) {
    return res.status(error.estadoHttp).json({
      mensaje: error.message,
      codigo: error.codigo
    });
  }

  console.error(`Error al ${operacion} fotografías:`, {
    codigo: error.code || 'ERROR_FOTOGRAFIAS'
  });
  return res.status(500).json({
    mensaje: 'No fue posible completar la operación con fotografías'
  });
}

async function listarFotografias(req, res) {
  try {
    const fotografias = await fotoPropiedadService.listarFotografias(
      req.params.id
    );

    return res.status(200).json({
      mensaje: 'Fotografías recuperadas correctamente',
      total: fotografias.length,
      datos: fotografias
    });
  } catch (error) {
    return responderError(error, res, 'consultar');
  }
}

async function subirFotografias(req, res) {
  try {
    const fotografias = await fotoPropiedadService.subirFotografias(
      req.params.id,
      req.usuario.id_usuario,
      req.files
    );

    return res.status(201).json({
      mensaje: 'Fotografías cargadas correctamente',
      total: fotografias.length,
      datos: fotografias
    });
  } catch (error) {
    return responderError(error, res, 'cargar');
  }
}

async function eliminarFotografia(req, res) {
  try {
    const fotografia = await fotoPropiedadService.eliminarFotografia(
      req.params.id,
      req.params.idFoto,
      req.usuario.id_usuario
    );

    return res.status(200).json({
      mensaje: 'Fotografía eliminada correctamente',
      datos: fotografia
    });
  } catch (error) {
    return responderError(error, res, 'eliminar');
  }
}

async function reordenarFotografias(req, res) {
  try {
    const fotografias = await fotoPropiedadService.reordenarFotografias(
      req.params.id,
      req.usuario.id_usuario,
      req.body?.orden_fotos,
      req.body?.id_portada
    );

    return res.status(200).json({
      mensaje: 'Orden y portada actualizados correctamente',
      total: fotografias.length,
      datos: fotografias
    });
  } catch (error) {
    return responderError(error, res, 'reordenar');
  }
}

async function obtenerArchivoFotografia(req, res) {
  try {
    const archivo = await fotoPropiedadService.obtenerArchivo(
      req.params.id,
      req.params.idFoto
    );

    res.set('Cache-Control', 'private, max-age=3600');
    res.type(archivo.mimeType);
    return res.sendFile(archivo.rutaAbsoluta);
  } catch (error) {
    return responderError(error, res, 'obtener el archivo de');
  }
}

module.exports = {
  exigirPropietario,
  listarFotografias,
  subirFotografias,
  eliminarFotografia,
  reordenarFotografias,
  obtenerArchivoFotografia
};

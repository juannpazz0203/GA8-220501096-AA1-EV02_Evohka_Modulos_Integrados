const fallaService = require('../services/fallaService');

const ESTADOS = ['pendiente', 'en_proceso', 'resuelta'];

function idValido(valor) {
  const id = Number(valor);
  return Number.isInteger(id) && id > 0 ? id : null;
}

function texto(valor, maximo) {
  const normalizado = typeof valor === 'string' ? valor.trim() : '';
  return normalizado && normalizado.length <= maximo
    ? normalizado
    : null;
}

async function listarFallas(req, res) {
  try {
    if (!['arrendatario', 'propietario'].includes(req.usuario?.rol)) {
      return res.status(403).json({ mensaje: 'El rol autenticado no puede consultar fallas' });
    }

    const datos = await fallaService.listarFallas(
      req.usuario.id_usuario,
      req.usuario.rol
    );

    return res.status(200).json({
      mensaje: 'Fallas recuperadas correctamente',
      total: datos.length,
      datos
    });
  } catch (error) {
    console.error('Error al listar fallas:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible consultar las fallas' });
  }
}

async function listarPropiedadesHabilitadas(req, res) {
  try {
    if (req.usuario?.rol !== 'arrendatario') {
      return res.status(403).json({ mensaje: 'Solo los arrendatarios pueden reportar fallas' });
    }

    const datos = await fallaService.listarPropiedadesHabilitadas(
      req.usuario.id_usuario
    );

    return res.status(200).json({
      mensaje: 'Propiedades habilitadas recuperadas correctamente',
      total: datos.length,
      datos
    });
  } catch (error) {
    console.error('Error al listar propiedades para fallas:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible consultar las propiedades habilitadas' });
  }
}

async function crearFalla(req, res) {
  try {
    if (req.usuario?.rol !== 'arrendatario') {
      return res.status(403).json({ mensaje: 'Solo los arrendatarios pueden reportar fallas' });
    }

    const idPropiedad = idValido(req.body?.id_propiedad);
    const categoria = texto(req.body?.categoria, 100);
    const descripcion = texto(req.body?.descripcion, 255);

    if (!idPropiedad || !categoria || !descripcion) {
      return res.status(400).json({
        mensaje: 'La propiedad, categoría y descripción son obligatorias y deben ser válidas'
      });
    }

    const resultado = await fallaService.crearFalla({
      id_usuario: req.usuario.id_usuario,
      id_propiedad: idPropiedad,
      categoria,
      descripcion
    });

    if (resultado.error === 'sin_relacion_activa') {
      return res.status(403).json({
        mensaje: 'No existe un contrato activo que permita reportar fallas en esta propiedad'
      });
    }

    return res.status(201).json({
      mensaje: 'Falla reportada correctamente',
      datos: resultado.datos
    });
  } catch (error) {
    console.error('Error al crear falla:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible reportar la falla' });
  }
}

async function actualizarEstadoFalla(req, res) {
  try {
    if (req.usuario?.rol !== 'propietario') {
      return res.status(403).json({ mensaje: 'Solo los propietarios pueden actualizar fallas' });
    }

    const idFalla = idValido(req.params.id);
    const estado = typeof req.body?.estado === 'string'
      ? req.body.estado.trim().toLowerCase()
      : '';

    if (!idFalla || !ESTADOS.includes(estado)) {
      return res.status(400).json({
        mensaje: 'El identificador o el estado de la falla no es válido'
      });
    }

    const resultado = await fallaService.actualizarEstadoFalla(
      idFalla,
      req.usuario.id_usuario,
      estado
    );

    const errores = {
      no_existe: [404, 'La falla no existe'],
      sin_permiso: [403, 'No tiene permiso para actualizar esta falla'],
      ya_resuelta: [409, 'Una falla resuelta no puede cambiar de estado']
    };

    if (resultado.error) {
      const [codigo, mensaje] = errores[resultado.error] || [409, 'No fue posible actualizar la falla'];
      return res.status(codigo).json({ mensaje });
    }

    return res.status(200).json({
      mensaje: 'Estado de la falla actualizado correctamente',
      datos: resultado.datos
    });
  } catch (error) {
    console.error('Error al actualizar falla:', error.message);
    return res.status(500).json({ mensaje: 'No fue posible actualizar la falla' });
  }
}

module.exports = {
  listarFallas,
  listarPropiedadesHabilitadas,
  crearFalla,
  actualizarEstadoFalla
};

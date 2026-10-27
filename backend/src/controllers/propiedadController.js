const propiedadService = require('../services/propiedadService');

function esPropietarioAutenticado(req) {
  return req.usuario?.rol === 'propietario';
}

function obtenerIdPropiedad(valor) {
  const idPropiedad = Number(valor);

  if (!Number.isInteger(idPropiedad) || idPropiedad <= 0) {
    return null;
  }

  return idPropiedad;
}

function validarDatosPropiedad(datos = {}) {
  const {
    titulo,
    tipo_inmueble,
    direccion,
    barrio,
    ciudad,
    canon,
    habitaciones,
    banos
  } = datos;

  const camposTexto = [
    titulo,
    tipo_inmueble,
    direccion,
    barrio,
    ciudad
  ];

  if (
    camposTexto.some(
      (campo) => typeof campo !== 'string' || campo.trim() === ''
    ) ||
    canon === undefined ||
    habitaciones === undefined ||
    banos === undefined
  ) {
    return {
      error: 'Todos los campos de la propiedad son obligatorios'
    };
  }

  const canonNumero = Number(canon);
  const habitacionesNumero = Number(habitaciones);
  const banosNumero = Number(banos);

  if (!Number.isFinite(canonNumero) || canonNumero <= 0) {
    return {
      error: 'El canon debe ser un número mayor que cero'
    };
  }

  if (
    !Number.isInteger(habitacionesNumero) ||
    habitacionesNumero < 0
  ) {
    return {
      error: 'La cantidad de habitaciones debe ser un número entero válido'
    };
  }

  if (
    !Number.isInteger(banosNumero) ||
    banosNumero < 0
  ) {
    return {
      error: 'La cantidad de baños debe ser un número entero válido'
    };
  }

  return {
    datos: {
      titulo: titulo.trim(),
      tipo_inmueble: tipo_inmueble.trim(),
      direccion: direccion.trim(),
      barrio: barrio.trim(),
      ciudad: ciudad.trim(),
      canon: canonNumero,
      habitaciones: habitacionesNumero,
      banos: banosNumero
    }
  };
}

async function listarPropiedades(req, res) {
  try {
    const propiedades = await propiedadService.obtenerTodasLasPropiedades();

    return res.status(200).json({
      mensaje: 'Lista de propiedades recuperada con éxito',
      total: propiedades.length,
      datos: propiedades
    });
  } catch (error) {
    console.error('Error al listar propiedades:', error.message);

    return res.status(500).json({
      mensaje: 'No fue posible consultar las propiedades',
      error: error.message
    });
  }
}

async function registrarPropiedad(req, res) {
  try {
    if (!esPropietarioAutenticado(req)) {
      return res.status(403).json({
        mensaje: 'Solo los usuarios con rol propietario pueden registrar propiedades'
      });
    }

    const validacion = validarDatosPropiedad(req.body);

    if (validacion.error) {
      return res.status(400).json({
        mensaje: validacion.error
      });
    }

    const propiedadCreada = await propiedadService.crearPropiedad({
      ...validacion.datos,
      id_propietario: req.usuario.id_usuario
    });

    return res.status(201).json({
      mensaje: 'Propiedad registrada correctamente',
      datos: propiedadCreada
    });
  } catch (error) {
    console.error('Error al registrar propiedad:', error.message);

    return res.status(500).json({
      mensaje: 'No fue posible registrar la propiedad',
      error: error.message
    });
  }
}

async function actualizarPropiedad(req, res) {
  try {
    if (!esPropietarioAutenticado(req)) {
      return res.status(403).json({
        mensaje: 'Solo los usuarios con rol propietario pueden editar propiedades'
      });
    }

    const idPropiedad = obtenerIdPropiedad(req.params.id);

    if (!idPropiedad) {
      return res.status(400).json({
        mensaje: 'El identificador de la propiedad no es válido'
      });
    }

    const validacion = validarDatosPropiedad(req.body);

    if (validacion.error) {
      return res.status(400).json({
        mensaje: validacion.error
      });
    }

    const propiedadExistente =
      await propiedadService.obtenerPropiedadPorId(idPropiedad);

    if (!propiedadExistente) {
      return res.status(404).json({
        mensaje: 'La propiedad no existe'
      });
    }

    if (
      Number(propiedadExistente.id_propietario) !==
      Number(req.usuario.id_usuario)
    ) {
      return res.status(403).json({
        mensaje: 'No tiene permiso para editar esta propiedad'
      });
    }

    const propiedadActualizada =
      await propiedadService.actualizarPropiedad(
        idPropiedad,
        req.usuario.id_usuario,
        validacion.datos
      );

    if (!propiedadActualizada) {
      return res.status(404).json({
        mensaje: 'La propiedad ya no está disponible'
      });
    }

    return res.status(200).json({
      mensaje: 'Propiedad actualizada correctamente',
      datos: propiedadActualizada
    });
  } catch (error) {
    console.error('Error al actualizar propiedad:', error.message);

    return res.status(500).json({
      mensaje: 'No fue posible actualizar la propiedad'
    });
  }
}

async function eliminarPropiedad(req, res) {
  try {
    if (!esPropietarioAutenticado(req)) {
      return res.status(403).json({
        mensaje: 'Solo los usuarios con rol propietario pueden eliminar propiedades'
      });
    }

    const idPropiedad = obtenerIdPropiedad(req.params.id);

    if (!idPropiedad) {
      return res.status(400).json({
        mensaje: 'El identificador de la propiedad no es válido'
      });
    }

    const propiedadExistente =
      await propiedadService.obtenerPropiedadPorId(idPropiedad);

    if (!propiedadExistente) {
      return res.status(404).json({
        mensaje: 'La propiedad no existe'
      });
    }

    if (
      Number(propiedadExistente.id_propietario) !==
      Number(req.usuario.id_usuario)
    ) {
      return res.status(403).json({
        mensaje: 'No tiene permiso para eliminar esta propiedad'
      });
    }

    const propiedadEliminada =
      await propiedadService.eliminarPropiedad(
        idPropiedad,
        req.usuario.id_usuario
      );

    if (!propiedadEliminada) {
      return res.status(404).json({
        mensaje: 'La propiedad ya no está disponible'
      });
    }

    return res.status(200).json({
      mensaje: 'Propiedad eliminada correctamente',
      datos: propiedadExistente
    });
  } catch (error) {
    if (
      error.code === 'ER_ROW_IS_REFERENCED_2' ||
      error.errno === 1451
    ) {
      return res.status(409).json({
        mensaje: 'No se puede eliminar la propiedad porque tiene información relacionada'
      });
    }

    console.error('Error al eliminar propiedad:', error.message);

    return res.status(500).json({
      mensaje: 'No fue posible eliminar la propiedad'
    });
  }
}

module.exports = {
  listarPropiedades,
  registrarPropiedad,
  actualizarPropiedad,
  eliminarPropiedad
};

const propiedadService = require('../services/propiedadService');

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
    const {
      titulo,
      tipo_inmueble,
      direccion,
      barrio,
      ciudad,
      canon,
      habitaciones,
      banos
    } = req.body;

    const id_propietario = req.usuario.id_usuario;

    if (
      !titulo ||
      !tipo_inmueble ||
      !direccion ||
      !barrio ||
      !ciudad ||
      canon === undefined ||
      habitaciones === undefined ||
      banos === undefined
    ) {
      return res.status(400).json({
        mensaje: 'Todos los campos de la propiedad son obligatorios'
      });
    }

    const canonNumero = Number(canon);
    const habitacionesNumero = Number(habitaciones);
    const banosNumero = Number(banos);

    if (!Number.isFinite(canonNumero) || canonNumero <= 0) {
      return res.status(400).json({
        mensaje: 'El canon debe ser un número mayor que cero'
      });
    }

    if (
      !Number.isInteger(habitacionesNumero) ||
      habitacionesNumero < 0
    ) {
      return res.status(400).json({
        mensaje: 'La cantidad de habitaciones debe ser un número entero válido'
      });
    }

    if (
      !Number.isInteger(banosNumero) ||
      banosNumero < 0
    ) {
      return res.status(400).json({
        mensaje: 'La cantidad de baños debe ser un número entero válido'
      });
    }

    const propiedadCreada = await propiedadService.crearPropiedad({
      titulo,
      tipo_inmueble,
      direccion,
      barrio,
      ciudad,
      canon: canonNumero,
      habitaciones: habitacionesNumero,
      banos: banosNumero,
      id_propietario
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

module.exports = {
  listarPropiedades,
  registrarPropiedad
};

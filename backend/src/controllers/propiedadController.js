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
    const { titulo, ciudad, direccion, precio_mensual } = req.body;

    if (!titulo || !ciudad || !direccion || precio_mensual === undefined) {
      return res.status(400).json({
        mensaje: 'Los campos titulo, ciudad, direccion y precio_mensual son obligatorios'
      });
    }

    const precio = Number(precio_mensual);
    if (!Number.isFinite(precio) || precio <= 0) {
      return res.status(400).json({
        mensaje: 'El precio_mensual debe ser un número mayor que cero'
      });
    }

    const propiedadCreada = await propiedadService.crearPropiedad({
      titulo,
      ciudad,
      direccion,
      precio_mensual: precio
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

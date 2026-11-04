const adminService = require('../services/adminService');

async function obtenerResumen(req, res) {
  try {
    if (req.usuario?.rol !== 'administrador') {
      return res.status(403).json({
        mensaje: 'Se requiere el rol administrador'
      });
    }

    const datos = await adminService.obtenerResumen();
    return res.status(200).json({
      mensaje: 'Resumen administrativo recuperado correctamente',
      datos
    });
  } catch (error) {
    console.error('Error al consultar resumen administrativo:', error.message);
    return res.status(500).json({
      mensaje: 'No fue posible consultar el resumen administrativo'
    });
  }
}

module.exports = { obtenerResumen };

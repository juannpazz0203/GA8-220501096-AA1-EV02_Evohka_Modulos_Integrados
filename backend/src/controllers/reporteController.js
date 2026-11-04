const reporteService = require('../services/reporteService');

async function obtenerReporteIngresos(req, res) {
  try {
    if (req.usuario?.rol !== 'propietario') {
      return res.status(403).json({
        mensaje: 'Solo los propietarios pueden consultar reportes de ingresos'
      });
    }

    const reporte =
      await reporteService.obtenerReporteIngresos(
        req.usuario.id_usuario
      );

    return res.status(200).json({
      mensaje: 'Reporte de ingresos generado correctamente',
      datos: reporte
    });
  } catch (error) {
    console.error(
      'Error al generar reporte de ingresos:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible generar el reporte de ingresos'
    });
  }
}

module.exports = {
  obtenerReporteIngresos
};

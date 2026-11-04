const { Router } = require('express');
const reporteController = require('../controllers/reporteController');
const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

router.get(
  '/ingresos',
  verificarToken,
  reporteController.obtenerReporteIngresos
);

module.exports = router;

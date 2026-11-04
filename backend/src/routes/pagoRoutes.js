const { Router } = require('express');
const pagoController = require('../controllers/pagoController');
const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

router.get(
  '/',
  verificarToken,
  pagoController.listarPagos
);

router.post(
  '/',
  verificarToken,
  pagoController.crearPago
);

module.exports = router;

const { Router } = require('express');
const contratoController = require('../controllers/contratoController');
const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

router.get(
  '/',
  verificarToken,
  contratoController.listarContratos
);

router.post(
  '/',
  verificarToken,
  contratoController.crearContrato
);

router.patch(
  '/:id/estado',
  verificarToken,
  contratoController.actualizarEstadoContrato
);

module.exports = router;

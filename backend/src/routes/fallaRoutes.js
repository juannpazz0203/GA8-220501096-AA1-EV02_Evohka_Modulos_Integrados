const { Router } = require('express');
const fallaController = require('../controllers/fallaController');
const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

router.get('/', verificarToken, fallaController.listarFallas);
router.get(
  '/propiedades',
  verificarToken,
  fallaController.listarPropiedadesHabilitadas
);
router.post('/', verificarToken, fallaController.crearFalla);
router.patch(
  '/:id/estado',
  verificarToken,
  fallaController.actualizarEstadoFalla
);

module.exports = router;

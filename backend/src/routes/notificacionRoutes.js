const { Router } = require('express');
const notificacionController = require('../controllers/notificacionController');
const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

router.get(
  '/',
  verificarToken,
  notificacionController.listarNotificaciones
);

router.patch(
  '/:id/leida',
  verificarToken,
  notificacionController.marcarComoLeida
);

module.exports = router;

const { Router } = require('express');

const {
  crearSolicitud,
  listarSolicitudesRecibidas,
  actualizarEstadoSolicitud
} = require('../controllers/solicitudController');

const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

// POST /api/solicitudes - Arrendatario autenticado
router.post('/', verificarToken, crearSolicitud);

// GET /api/solicitudes/recibidas - Propietario autenticado
router.get(
  '/recibidas',
  verificarToken,
  listarSolicitudesRecibidas
);

// PATCH /api/solicitudes/:id/estado - Propietario autenticado
router.patch(
  '/:id/estado',
  verificarToken,
  actualizarEstadoSolicitud
);

module.exports = router;

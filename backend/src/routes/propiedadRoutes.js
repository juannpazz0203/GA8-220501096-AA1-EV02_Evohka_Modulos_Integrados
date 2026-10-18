const { Router } = require('express');

const {
  listarPropiedades,
  registrarPropiedad
} = require('../controllers/propiedadController');

const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

// GET /api/propiedades - Ruta protegida
router.get('/', verificarToken, listarPropiedades);

// POST /api/propiedades - Ruta protegida
router.post('/', verificarToken, registrarPropiedad);

module.exports = router;
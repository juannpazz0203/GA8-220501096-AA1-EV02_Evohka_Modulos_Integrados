const { Router } = require('express');

const {
  listarPropiedades,
  registrarPropiedad
} = require('../controllers/propiedadController');

const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

// GET /api/propiedades - Ruta protegida
router.get('/', verificarToken, listarPropiedades);

// POST /api/propiedades
router.post('/', registrarPropiedad);

module.exports = router;
const { Router } = require('express');

const {
  listarPropiedades,
  registrarPropiedad,
  actualizarPropiedad,
  eliminarPropiedad
} = require('../controllers/propiedadController');

const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

// GET /api/propiedades - Ruta protegida
router.get('/', verificarToken, listarPropiedades);

// POST /api/propiedades - Ruta protegida
router.post('/', verificarToken, registrarPropiedad);

// PUT /api/propiedades/:id - Ruta protegida
router.put('/:id', verificarToken, actualizarPropiedad);

// DELETE /api/propiedades/:id - Ruta protegida
router.delete('/:id', verificarToken, eliminarPropiedad);

module.exports = router;

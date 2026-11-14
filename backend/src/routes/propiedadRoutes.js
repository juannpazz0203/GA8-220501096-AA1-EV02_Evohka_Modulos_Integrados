const { Router } = require('express');

const {
  listarPropiedades,
  registrarPropiedad,
  actualizarPropiedad,
  eliminarPropiedad
} = require('../controllers/propiedadController');

const { verificarToken } = require('../middlewares/authMiddleware');
const {
  recibirFotografias
} = require('../middlewares/cargaFotografiasMiddleware');
const {
  exigirPropietario,
  listarFotografias,
  subirFotografias,
  eliminarFotografia,
  reordenarFotografias,
  obtenerArchivoFotografia
} = require('../controllers/fotoPropiedadController');

const router = Router();

// GET /api/propiedades - Ruta protegida
router.get('/', verificarToken, listarPropiedades);

// GET /api/propiedades/:id/fotos - Fotografías de una propiedad
router.get('/:id/fotos', verificarToken, listarFotografias);

// GET /api/propiedades/:id/fotos/:idFoto/archivo - WebP optimizado
router.get(
  '/:id/fotos/:idFoto/archivo',
  verificarToken,
  obtenerArchivoFotografia
);

// POST /api/propiedades/:id/fotos - Solo el propietario del inmueble
router.post(
  '/:id/fotos',
  verificarToken,
  exigirPropietario,
  recibirFotografias,
  subirFotografias
);

// PUT /api/propiedades/:id/fotos/orden - Orden completo y portada
router.put(
  '/:id/fotos/orden',
  verificarToken,
  exigirPropietario,
  reordenarFotografias
);

// DELETE /api/propiedades/:id/fotos/:idFoto - Solo propietario
router.delete(
  '/:id/fotos/:idFoto',
  verificarToken,
  exigirPropietario,
  eliminarFotografia
);

// POST /api/propiedades - Ruta protegida
router.post('/', verificarToken, registrarPropiedad);

// PUT /api/propiedades/:id - Ruta protegida
router.put('/:id', verificarToken, actualizarPropiedad);

// DELETE /api/propiedades/:id - Ruta protegida
router.delete('/:id', verificarToken, eliminarPropiedad);

module.exports = router;

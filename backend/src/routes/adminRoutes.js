const { Router } = require('express');
const adminController = require('../controllers/adminController');
const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

router.get('/resumen', verificarToken, adminController.obtenerResumen);
router.get('/usuarios', verificarToken, adminController.listarUsuarios);
router.post('/usuarios', verificarToken, adminController.crearUsuario);
router.put('/usuarios/:id', verificarToken, adminController.actualizarUsuario);
router.delete('/usuarios/:id', verificarToken, adminController.eliminarUsuario);

module.exports = router;

const { Router } = require('express');
const adminController = require('../controllers/adminController');
const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

router.get('/resumen', verificarToken, adminController.obtenerResumen);

module.exports = router;

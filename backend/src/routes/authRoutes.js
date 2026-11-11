const express = require('express');
const authController = require('../controllers/authController');
const recuperacionController = require('../controllers/recuperacionController');
const { verificarToken } = require('../middlewares/authMiddleware');

const router = express.Router();

router.post('/login', authController.login);
router.post('/registro', authController.registro);
router.post('/recuperacion/solicitar', recuperacionController.solicitar);
router.post('/recuperacion/restablecer', recuperacionController.restablecer);
router.get('/sesion', verificarToken, authController.sesion);

module.exports = router;

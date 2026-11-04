const { Router } = require('express');
const mensajeController = require('../controllers/mensajeController');
const { verificarToken } = require('../middlewares/authMiddleware');

const router = Router();

router.get('/conversaciones', verificarToken, mensajeController.listarConversaciones);
router.get('/', verificarToken, mensajeController.listarMensajes);
router.post('/', verificarToken, mensajeController.enviarMensaje);
router.patch('/leidos', verificarToken, mensajeController.marcarLeidos);

module.exports = router;

const jwt = require('jsonwebtoken');
const authService = require('../services/authService');

const login = async (req, res) => {
    try {
        const { correo, contrasena } = req.body;

        if (!correo || !contrasena) {
            return res.status(400).json({
                ok: false,
                mensaje: 'Debe proporcionar el correo y la contraseña'
            });
        }

        const usuario = await authService.validarCredenciales(
            correo,
            contrasena
        );

        if (!usuario) {
            return res.status(401).json({
                ok: false,
                mensaje: 'Correo o contraseña incorrectos'
            });
        }

        const token = jwt.sign(
            {
                id_usuario: usuario.id_usuario,
                correo: usuario.correo,
                rol: usuario.rol
            },
            process.env.JWT_SECRET,
            {
                expiresIn: '2h'
            }
        );

        return res.status(200).json({
            ok: true,
            mensaje: 'Autenticación exitosa',
            usuario,
            token: `Bearer ${token}`
        });
    } catch (error) {
        console.error('Error en el inicio de sesión:', error);

        return res.status(500).json({
            ok: false,
            mensaje: 'Error interno del servidor'
        });
    }
};

module.exports = {
    login
};


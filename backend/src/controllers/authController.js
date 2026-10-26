const jwt = require('jsonwebtoken');
const authService = require('../services/authService');

const login = async (req, res) => {
    try {
        const { correo, contrasena } = req.body || {};

        if (!correo || !contrasena) {
            return res.status(400).json({
                ok: false,
                mensaje: 'Debe proporcionar el correo y la contrasena'
            });
        }

        const usuario = await authService.validarCredenciales(
            correo,
            contrasena
        );

        if (!usuario) {
            return res.status(401).json({
                ok: false,
                mensaje: 'Correo o contrasena incorrectos'
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
            mensaje: 'Autenticacion exitosa',
            usuario,
            token: `Bearer ${token}`
        });
    } catch (error) {
        console.error('Error en el inicio de sesion:', error);

        return res.status(500).json({
            ok: false,
            mensaje: 'Error interno del servidor'
        });
    }
};

const registro = async (req, res) => {
    try {
        const {
            nombre,
            apellido,
            correo,
            contrasena,
            telefono,
            rol
        } = req.body || {};

        if (
            !nombre ||
            !apellido ||
            !correo ||
            !contrasena ||
            !rol
        ) {
            return res.status(400).json({
                ok: false,
                mensaje:
                    'Nombre, apellido, correo, contrasena y rol son obligatorios'
            });
        }

        const correoNormalizado =
            correo.trim().toLowerCase();

        const rolRecibido =
            rol.trim().toLowerCase();

        const rolNormalizado =
            rolRecibido === 'inquilino'
                ? 'arrendatario'
                : rolRecibido;

        if (
            rolNormalizado !== 'arrendatario' &&
            rolNormalizado !== 'propietario'
        ) {
            return res.status(400).json({
                ok: false,
                mensaje:
                    'El rol debe ser arrendatario o propietario'
            });
        }

        if (contrasena.length < 6) {
            return res.status(400).json({
                ok: false,
                mensaje:
                    'La contrasena debe tener minimo 6 caracteres'
            });
        }

        const correoExiste =
            await authService.buscarUsuarioPorCorreo(
                correoNormalizado
            );

        if (correoExiste) {
            return res.status(409).json({
                ok: false,
                mensaje:
                    'Ya existe un usuario registrado con ese correo'
            });
        }

        const usuario =
            await authService.registrarUsuario({
                nombre,
                apellido,
                correo: correoNormalizado,
                contrasena,
                telefono,
                rol: rolNormalizado
            });

        return res.status(201).json({
            ok: true,
            mensaje: 'Usuario registrado correctamente',
            usuario
        });
    } catch (error) {
        console.error(
            'Error en el registro de usuario:',
            error
        );

        return res.status(500).json({
            ok: false,
            mensaje: 'Error interno del servidor'
        });
    }
};

module.exports = {
    login,
    registro
};
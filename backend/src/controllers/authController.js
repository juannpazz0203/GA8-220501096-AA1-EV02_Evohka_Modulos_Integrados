const authService = require('../services/authService');
const contrasenaService = require('../services/contrasenaService');
const jwtService = require('../services/jwtService');

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

        const token = jwtService.crearToken(usuario);
        const {
            sesion_version: sesionVersion,
            ...usuarioPublico
        } = usuario;

        return res.status(200).json({
            ok: true,
            mensaje: 'Autenticacion exitosa',
            usuario: usuarioPublico,
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

const sesion = async (req, res) => {
    const {
        sesion_version: sesionVersion,
        ...usuario
    } = req.usuario;

    return res.status(200).json({
        ok: true,
        mensaje: 'Sesión válida',
        usuario
    });
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

        const validacionContrasena =
            contrasenaService.validarNuevaContrasena(contrasena);

        if (!validacionContrasena.valida) {
            return res.status(400).json({
                ok: false,
                mensaje: validacionContrasena.mensaje
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
    registro,
    sesion
};

const authService = require('../services/authService');
const contrasenaService = require('../services/contrasenaService');
const jwtService = require('../services/jwtService');
const {
    normalizarCorreo,
    correoValido
} = require('../utils/correo');

const ROLES_REGISTRO_PUBLICO = new Set([
    'arrendatario',
    'propietario'
]);

function validarRegistroPublico(datos = {}) {
    const nombre = typeof datos.nombre === 'string'
        ? datos.nombre.trim()
        : '';
    const apellido = typeof datos.apellido === 'string'
        ? datos.apellido.trim()
        : '';
    const correo = normalizarCorreo(datos.correo);
    const contrasena = typeof datos.contrasena === 'string'
        ? datos.contrasena
        : '';
    const rolRecibido = typeof datos.rol === 'string'
        ? datos.rol.trim().toLowerCase()
        : '';
    const rol = rolRecibido === 'inquilino'
        ? 'arrendatario'
        : rolRecibido;

    if (!nombre || nombre.length > 100) {
        return {
            error: 'El nombre es obligatorio y no puede superar 100 caracteres'
        };
    }
    if (!apellido || apellido.length > 100) {
        return {
            error: 'El apellido es obligatorio y no puede superar 100 caracteres'
        };
    }
    if (!correoValido(correo)) {
        return { error: 'El correo no tiene un formato válido' };
    }
    if (!ROLES_REGISTRO_PUBLICO.has(rol)) {
        return {
            error: 'El rol debe ser arrendatario o propietario'
        };
    }

    const validacionContrasena =
        contrasenaService.validarNuevaContrasena(contrasena);
    if (!validacionContrasena.valida) {
        return { error: validacionContrasena.mensaje };
    }

    let telefono = null;
    if (datos.telefono !== undefined && datos.telefono !== null) {
        if (typeof datos.telefono !== 'string') {
            return { error: 'El teléfono debe ser texto' };
        }
        telefono = datos.telefono.trim() || null;
        if (telefono && telefono.length > 20) {
            return {
                error: 'El teléfono no puede superar 20 caracteres'
            };
        }
    }

    return {
        datos: {
            nombre,
            apellido,
            correo,
            contrasena,
            telefono,
            rol
        }
    };
}

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
        const validacion = validarRegistroPublico(req.body);
        if (validacion.error) {
            return res.status(400).json({
                ok: false,
                mensaje: validacion.error
            });
        }
        const datos = validacion.datos;

        const correoExiste =
            await authService.buscarUsuarioPorCorreo(
                datos.correo
            );

        if (correoExiste) {
            return res.status(409).json({
                ok: false,
                mensaje:
                    'Ya existe un usuario registrado con ese correo'
            });
        }

        const usuario =
            await authService.registrarUsuario(datos);

        if (usuario.error === 'correo_duplicado') {
            return res.status(409).json({
                ok: false,
                mensaje:
                    'Ya existe un usuario registrado con ese correo'
            });
        }

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
    sesion,
    validarRegistroPublico
};

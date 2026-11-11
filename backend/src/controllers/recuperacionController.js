const contrasenaService = require('../services/contrasenaService');
const {
  CODIGO_DIGITOS,
  ERROR_CONFIGURACION_RECUPERACION
} = require('../config/recuperacion');
const {
  ESTADO_EXITOSO,
  recuperacionService
} = require('../services/recuperacionService');
const {
  recuperacionLimiteService
} = require('../services/recuperacionLimiteService');

const MENSAJE_NEUTRAL =
  'Si el correo está registrado, recibirá un código de recuperación.';
const MENSAJE_CODIGO_INVALIDO = 'El código es inválido o venció.';

function correoValido(correo) {
  return typeof correo === 'string' &&
    correo.trim().length <= 150 &&
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim());
}

function ipSolicitud(req) {
  return req.ip || req.socket?.remoteAddress || 'desconocida';
}

function crearRecuperacionController({
  servicio = recuperacionService,
  limites = recuperacionLimiteService,
  programar = (operacion) => setImmediate(operacion),
  registrarError = (codigo) => console.error(
    'Error en recuperación diferida:',
    { codigo }
  )
} = {}) {
  async function solicitar(req, res) {
    try {
      servicio.verificarConfiguracion();
    } catch (error) {
      return res.status(503).json({
        ok: false,
        mensaje: 'El servicio de recuperación no está disponible'
      });
    }

    const correo = req.body?.correo;
    const permitida = limites.consumirSolicitud({
      correo,
      ip: ipSolicitud(req)
    });

    if (permitida && correoValido(correo)) {
      programar(() => {
        servicio.solicitarRecuperacion(correo).catch((error) => {
          registrarError(error.code || 'ERROR_RECUPERACION_DIFERIDA');
        });
      });
    }

    return res.status(202).json({
      ok: true,
      mensaje: MENSAJE_NEUTRAL
    });
  }

  async function restablecer(req, res) {
    try {
      servicio.verificarConfiguracion();
    } catch (error) {
      return res.status(503).json({
        ok: false,
        mensaje: 'El servicio de recuperación no está disponible'
      });
    }

    const {
      correo,
      codigo,
      nueva_contrasena: nuevaContrasena
    } = req.body || {};
    const codigoValido = typeof codigo === 'string' &&
      new RegExp(`^\\d{${CODIGO_DIGITOS}}$`).test(codigo);
    const validacionContrasena =
      contrasenaService.validarNuevaContrasena(nuevaContrasena);

    if (!correoValido(correo) || !codigoValido) {
      return res.status(400).json({
        ok: false,
        mensaje: MENSAJE_CODIGO_INVALIDO
      });
    }

    if (!validacionContrasena.valida) {
      return res.status(400).json({
        ok: false,
        mensaje: validacionContrasena.mensaje
      });
    }

    if (!limites.consumirRestablecimiento({
      correo,
      ip: ipSolicitud(req)
    })) {
      return res.status(429).json({
        ok: false,
        mensaje: 'Demasiados intentos. Intente nuevamente más tarde.'
      });
    }

    try {
      const resultado = await servicio.restablecerContrasena({
        correo,
        codigo,
        nuevaContrasena
      });

      if (resultado.estado !== ESTADO_EXITOSO) {
        return res.status(400).json({
          ok: false,
          mensaje: MENSAJE_CODIGO_INVALIDO
        });
      }

      return res.status(200).json({
        ok: true,
        mensaje: 'Contraseña restablecida correctamente'
      });
    } catch (error) {
      if (error.code === 'CONTRASENA_INVALIDA') {
        return res.status(400).json({
          ok: false,
          mensaje: error.message
        });
      }

      if (error.code === ERROR_CONFIGURACION_RECUPERACION) {
        return res.status(503).json({
          ok: false,
          mensaje: 'El servicio de recuperación no está disponible'
        });
      }

      console.error('Error al restablecer contraseña:', {
        codigo: error.code || 'ERROR_RESTABLECIMIENTO'
      });
      return res.status(500).json({
        ok: false,
        mensaje: 'No fue posible restablecer la contraseña'
      });
    }
  }

  return { solicitar, restablecer };
}

module.exports = {
  MENSAJE_NEUTRAL,
  MENSAJE_CODIGO_INVALIDO,
  crearRecuperacionController,
  ...crearRecuperacionController()
};

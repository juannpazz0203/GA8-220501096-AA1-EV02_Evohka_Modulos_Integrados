const crypto = require('node:crypto');
const {
  CODIGO_DIGITOS,
  VIGENCIA_CODIGO_MINUTOS,
  MAXIMO_INTENTOS_FALLIDOS,
  MAXIMO_SOLICITUDES_CUENTA,
  VENTANA_LIMITES_MS,
  obtenerConfiguracionRecuperacion
} = require('../config/recuperacion');
const contrasenaService = require('./contrasenaService');
const recuperacionRepository = require('../repositories/recuperacionRepository');
const {
  correoRecuperacionService
} = require('./correoRecuperacionService');

const ESTADO_INVALIDO = 'invalido';
const ESTADO_EXITOSO = 'exitoso';

function normalizarCorreo(correo) {
  return String(correo || '').trim().toLowerCase();
}

function generarCodigo() {
  return crypto
    .randomInt(0, 10 ** CODIGO_DIGITOS)
    .toString()
    .padStart(CODIGO_DIGITOS, '0');
}

function calcularHashCodigo(codigo, salt, pepper) {
  return crypto
    .createHmac('sha256', pepper)
    .update(salt)
    .update(':', 'utf8')
    .update(codigo, 'utf8')
    .digest();
}

function hashCoincide(codigo, salt, esperado, pepper) {
  if (!Buffer.isBuffer(salt) || !Buffer.isBuffer(esperado)) return false;
  const calculado = calcularHashCodigo(codigo, salt, pepper);
  return calculado.length === esperado.length &&
    crypto.timingSafeEqual(calculado, esperado);
}

function crearError(codigo, mensaje) {
  const error = new Error(mensaje);
  error.code = codigo;
  return error;
}

function crearRecuperacionService({
  repository = recuperacionRepository,
  servicioCorreo = correoRecuperacionService,
  servicioContrasena = contrasenaService,
  obtenerConfiguracion = obtenerConfiguracionRecuperacion,
  ahora = () => new Date()
} = {}) {
  function verificarConfiguracion() {
    return obtenerConfiguracion();
  }

  async function solicitarRecuperacion(correoRecibido) {
    const correo = normalizarCorreo(correoRecibido);
    const { pepper } = obtenerConfiguracion();
    const fechaCreacion = ahora();
    const fechaExpiracion = new Date(
      fechaCreacion.getTime() + VIGENCIA_CODIGO_MINUTOS * 60 * 1000
    );
    const desde = new Date(fechaCreacion.getTime() - VENTANA_LIMITES_MS);

    const preparacion = await repository.conTransaccion(async (ejecutor) => {
      const usuario = await repository.buscarUsuarioPorCorreo(
        correo,
        ejecutor,
        true
      );

      if (!usuario) {
        const saltSintetico = crypto.randomBytes(16);
        calcularHashCodigo(generarCodigo(), saltSintetico, pepper);
        return { estado: 'sin_cuenta' };
      }

      const recientes = await repository.listarSolicitudesRecientes(
        usuario.id_usuario,
        desde,
        ejecutor
      );
      if (recientes.length >= MAXIMO_SOLICITUDES_CUENTA) {
        return { estado: 'limitado' };
      }

      const codigo = generarCodigo();
      const salt = crypto.randomBytes(16);
      const codigoHash = calcularHashCodigo(codigo, salt, pepper);

      await repository.revocarCodigosActivos(
        usuario.id_usuario,
        fechaCreacion,
        ejecutor
      );
      const idRecuperacion = await repository.crearCodigo(
        {
          idUsuario: usuario.id_usuario,
          codigoHash,
          codigoSalt: salt,
          fechaCreacion,
          fechaExpiracion
        },
        ejecutor
      );

      return {
        estado: 'preparado',
        idRecuperacion,
        destinatario: usuario.correo,
        codigo
      };
    });

    if (preparacion.estado !== 'preparado') return preparacion;

    try {
      await servicioCorreo.enviarCodigo({
        destinatario: preparacion.destinatario,
        codigo: preparacion.codigo
      });
    } catch (error) {
      await repository.revocarCodigo(preparacion.idRecuperacion, ahora());
      throw crearError(
        'ENVIO_RECUPERACION_FALLIDO',
        'No fue posible enviar el código de recuperación'
      );
    }

    const marcado = await repository.marcarCodigoEnviado(
      preparacion.idRecuperacion,
      ahora()
    );
    return {
      estado: marcado ? 'enviado' : 'reemplazado'
    };
  }

  async function restablecerContrasena({
    correo: correoRecibido,
    codigo,
    nuevaContrasena
  }) {
    const correo = normalizarCorreo(correoRecibido);
    const { pepper } = obtenerConfiguracion();

    // El costo de bcrypt se paga antes de consultar la cuenta tanto para un
    // correo existente como para uno inexistente y fuera de la transacción.
    const hashNuevo = await servicioContrasena.crearHash(nuevaContrasena);

    return repository.conTransaccion(async (ejecutor) => {
      const instante = ahora();
      const usuario = await repository.buscarUsuarioPorCorreo(
        correo,
        ejecutor,
        true
      );

      if (!usuario) {
        const saltSintetico = crypto.randomBytes(16);
        calcularHashCodigo(codigo, saltSintetico, pepper);
        return { estado: ESTADO_INVALIDO };
      }

      const recuperacion = await repository.obtenerUltimoCodigo(
        usuario.id_usuario,
        ejecutor
      );
      if (!recuperacion) return { estado: ESTADO_INVALIDO };

      const expiracionMs = new Date(recuperacion.fecha_expiracion).getTime();
      const intentosFallidos = Number(recuperacion.intentos_fallidos);
      const vencida = !Number.isFinite(expiracionMs) ||
        expiracionMs <= instante.getTime();
      const noDisponible =
        !recuperacion.fecha_envio ||
        recuperacion.fecha_uso !== null ||
        recuperacion.fecha_revocacion !== null ||
        !Number.isInteger(intentosFallidos) ||
        intentosFallidos < 0 ||
        intentosFallidos >= MAXIMO_INTENTOS_FALLIDOS;

      if (vencida || noDisponible) {
        return { estado: ESTADO_INVALIDO };
      }

      if (!hashCoincide(
        codigo,
        recuperacion.codigo_salt,
        recuperacion.codigo_hash,
        pepper
      )) {
        const intentos = intentosFallidos + 1;
        await repository.registrarIntentoFallido(
          recuperacion.id_recuperacion,
          intentos,
          intentos >= MAXIMO_INTENTOS_FALLIDOS ? instante : null,
          ejecutor
        );
        return { estado: ESTADO_INVALIDO };
      }

      const actualizada = await repository.actualizarContrasenaYVersion(
        usuario.id_usuario,
        hashNuevo,
        ejecutor
      );
      const marcado = actualizada && await repository.marcarCodigoUsado(
        recuperacion.id_recuperacion,
        instante,
        ejecutor
      );

      if (!marcado) {
        throw crearError(
          'RESTABLECIMIENTO_NO_ATOMICO',
          'No fue posible completar el restablecimiento'
        );
      }

      await repository.revocarCodigosActivos(
        usuario.id_usuario,
        instante,
        ejecutor,
        recuperacion.id_recuperacion
      );
      return { estado: ESTADO_EXITOSO };
    });
  }

  return {
    verificarConfiguracion,
    solicitarRecuperacion,
    restablecerContrasena
  };
}

module.exports = {
  ESTADO_INVALIDO,
  ESTADO_EXITOSO,
  normalizarCorreo,
  generarCodigo,
  calcularHashCodigo,
  crearRecuperacionService,
  recuperacionService: crearRecuperacionService()
};

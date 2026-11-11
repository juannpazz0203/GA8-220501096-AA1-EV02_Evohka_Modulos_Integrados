const jwt = require('jsonwebtoken');
const {
  obtenerConfiguracionJwt
} = require('../config/seguridad');

function errorToken(mensaje) {
  return new jwt.JsonWebTokenError(mensaje);
}

function enteroNoNegativo(valor) {
  const numero = Number(valor);
  return Number.isSafeInteger(numero) && numero >= 0 ? numero : null;
}

function idValido(valor) {
  const numero = Number(valor);
  return Number.isSafeInteger(numero) && numero > 0 ? numero : null;
}

function crearToken(usuario) {
  const configuracion = obtenerConfiguracionJwt();
  const idUsuario = idValido(usuario.id_usuario);
  const sesionVersion = enteroNoNegativo(usuario.sesion_version ?? 0);

  if (!idUsuario || sesionVersion === null) {
    throw errorToken('Identidad de usuario inválida para emitir JWT');
  }

  return jwt.sign(
    {
      id_usuario: idUsuario,
      correo: usuario.correo,
      rol: usuario.rol,
      sv: sesionVersion
    },
    configuracion.secreto,
    {
      algorithm: configuracion.algoritmo,
      expiresIn: configuracion.expiracion,
      issuer: configuracion.issuer,
      audience: configuracion.audience,
      subject: String(idUsuario)
    }
  );
}

function normalizarTokenActual(payload) {
  if (!payload || typeof payload !== 'object') {
    throw errorToken('Contenido JWT inválido');
  }

  const idUsuario = idValido(payload.sub);
  const idCompatibilidad = idValido(payload.id_usuario);
  const sesionVersion = enteroNoNegativo(payload.sv);

  if (
    !idUsuario ||
    idCompatibilidad !== idUsuario ||
    sesionVersion === null
  ) {
    throw errorToken('Claims de sesión inválidos');
  }

  return {
    formato: 'actual',
    id_usuario: idUsuario,
    sesion_version: sesionVersion,
    payload
  };
}

function normalizarTokenLegacy(payload, configuracion) {
  const transicion = configuracion.transicionLegacy;

  if (!transicion.habilitada || !payload || typeof payload !== 'object') {
    throw errorToken('JWT legacy fuera de la transición autorizada');
  }

  if (
    payload.iss !== undefined ||
    payload.aud !== undefined ||
    payload.sub !== undefined ||
    payload.sv !== undefined
  ) {
    throw errorToken('El token no corresponde al formato JWT legacy');
  }

  const idUsuario = idValido(payload.id_usuario);
  const emitido = Number(payload.iat);
  const expira = Number(payload.exp);

  if (
    !idUsuario ||
    !Number.isSafeInteger(emitido) ||
    !Number.isSafeInteger(expira) ||
    expira <= emitido ||
    expira - emitido > configuracion.duracionSegundos ||
    expira * 1000 > transicion.hastaMs
  ) {
    throw errorToken('Vigencia JWT legacy inválida');
  }

  return {
    formato: 'legacy',
    id_usuario: idUsuario,
    sesion_version: 0,
    payload
  };
}

function verificarToken(token) {
  const configuracion = obtenerConfiguracionJwt();

  try {
    const payload = jwt.verify(token, configuracion.secreto, {
      algorithms: [configuracion.algoritmo],
      issuer: configuracion.issuer,
      audience: configuracion.audience
    });
    return normalizarTokenActual(payload);
  } catch (errorActual) {
    if (!configuracion.transicionLegacy.habilitada) throw errorActual;

    try {
      const payloadLegacy = jwt.verify(token, configuracion.secreto, {
        algorithms: [configuracion.algoritmo]
      });
      return normalizarTokenLegacy(payloadLegacy, configuracion);
    } catch (errorLegacy) {
      throw errorActual;
    }
  }
}

module.exports = {
  crearToken,
  verificarToken
};

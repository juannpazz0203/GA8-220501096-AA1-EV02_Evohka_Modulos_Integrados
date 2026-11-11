const ERROR_CONFIGURACION_JWT = 'CONFIGURACION_JWT_INVALIDA';

const JWT_ALGORITMO = 'HS256';
const JWT_ISSUER = 'evohka-api';
const JWT_AUDIENCE = 'evohka-android';
const JWT_EXPIRACION = '2h';
const JWT_DURACION_SEGUNDOS = 2 * 60 * 60;
const JWT_SECRETO_MINIMO_BYTES = 32;
const MARGEN_CONFIGURACION_LEGACY_MS = 60 * 1000;

function errorConfiguracion(mensaje) {
  const error = new Error(mensaje);
  error.code = ERROR_CONFIGURACION_JWT;
  return error;
}

function sesionesVersionadasActivas() {
  return process.env.EVOHKA_SESIONES_VERSIONADAS_ACTIVAS === 'true';
}

function obtenerTransicionLegacy(ahoraMs = Date.now()) {
  if (process.env.EVOHKA_JWT_TRANSICION_LEGACY_ACTIVA !== 'true') {
    return { habilitada: false, hastaMs: null };
  }

  const valorHasta = process.env.EVOHKA_JWT_TRANSICION_LEGACY_HASTA;
  const hastaMs = Date.parse(valorHasta || '');

  if (!Number.isFinite(hastaMs)) {
    throw errorConfiguracion(
      'La transición JWT legacy requiere EVOHKA_JWT_TRANSICION_LEGACY_HASTA en formato ISO válido'
    );
  }

  if (hastaMs <= ahoraMs) {
    return { habilitada: false, hastaMs };
  }

  const ventanaMaximaMs =
    JWT_DURACION_SEGUNDOS * 1000 + MARGEN_CONFIGURACION_LEGACY_MS;

  if (hastaMs - ahoraMs > ventanaMaximaMs) {
    throw errorConfiguracion(
      'La transición JWT legacy no puede superar la vigencia actual de dos horas'
    );
  }

  return { habilitada: true, hastaMs };
}

function obtenerConfiguracionJwt(ahoraMs = Date.now()) {
  const secreto = process.env.JWT_SECRET;

  if (
    typeof secreto !== 'string' ||
    Buffer.byteLength(secreto, 'utf8') < JWT_SECRETO_MINIMO_BYTES
  ) {
    throw errorConfiguracion(
      `JWT_SECRET es obligatorio y debe tener al menos ${JWT_SECRETO_MINIMO_BYTES} bytes UTF-8`
    );
  }

  return {
    secreto,
    algoritmo: JWT_ALGORITMO,
    issuer: JWT_ISSUER,
    audience: JWT_AUDIENCE,
    expiracion: JWT_EXPIRACION,
    duracionSegundos: JWT_DURACION_SEGUNDOS,
    transicionLegacy: obtenerTransicionLegacy(ahoraMs)
  };
}

function validarConfiguracionJwt() {
  obtenerConfiguracionJwt();
}

module.exports = {
  ERROR_CONFIGURACION_JWT,
  JWT_ALGORITMO,
  JWT_ISSUER,
  JWT_AUDIENCE,
  JWT_EXPIRACION,
  JWT_DURACION_SEGUNDOS,
  sesionesVersionadasActivas,
  obtenerConfiguracionJwt,
  validarConfiguracionJwt
};

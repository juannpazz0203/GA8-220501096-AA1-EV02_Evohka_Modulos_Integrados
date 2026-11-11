const bcrypt = require('bcryptjs');
const crypto = require('node:crypto');

const ALGORITMO_LEGACY = 'legacy_plain';
const ALGORITMO_BCRYPT = 'bcrypt';
const MARCADOR_BCRYPT_V1 = '{EVOHKA_BCRYPT_V1}';
const LONGITUD_MINIMA = 6;
const MAXIMO_BYTES_BCRYPT = 72;
const COSTO_PREDETERMINADO = 12;

function costoBcrypt() {
  const configurado = Number(process.env.EVOHKA_BCRYPT_COSTO);
  return Number.isInteger(configurado) && configurado >= 10 && configurado <= 14
    ? configurado
    : COSTO_PREDETERMINADO;
}

function bytesUtf8(valor) {
  return Buffer.byteLength(valor, 'utf8');
}

function validarNuevaContrasena(contrasena) {
  if (typeof contrasena !== 'string' || contrasena.length < LONGITUD_MINIMA) {
    return {
      valida: false,
      mensaje: `La contraseña debe tener mínimo ${LONGITUD_MINIMA} caracteres`
    };
  }

  if (bytesUtf8(contrasena) > MAXIMO_BYTES_BCRYPT) {
    return {
      valida: false,
      mensaje: `La contraseña no puede superar ${MAXIMO_BYTES_BCRYPT} bytes UTF-8`
    };
  }

  return { valida: true };
}

function crearErrorPolitica(mensaje) {
  const error = new Error(mensaje);
  error.code = 'CONTRASENA_INVALIDA';
  return error;
}

async function crearHash(contrasena) {
  const validacion = validarNuevaContrasena(contrasena);
  if (!validacion.valida) {
    throw crearErrorPolitica(validacion.mensaje);
  }

  return bcrypt.hash(contrasena, costoBcrypt());
}

async function crearHashParaMigracion(contrasena) {
  if (
    typeof contrasena !== 'string' ||
    contrasena.length === 0 ||
    bytesUtf8(contrasena) > MAXIMO_BYTES_BCRYPT
  ) {
    return null;
  }

  return bcrypt.hash(contrasena, costoBcrypt());
}

function envolverHashParaEsquemaLegacy(hash) {
  return `${MARCADOR_BCRYPT_V1}${hash}`;
}

function extraerHashEnvuelto(credencial) {
  if (
    typeof credencial !== 'string' ||
    !credencial.startsWith(MARCADOR_BCRYPT_V1)
  ) {
    return null;
  }

  const hash = credencial.slice(MARCADOR_BCRYPT_V1.length);
  return /^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$/.test(hash)
    ? hash
    : null;
}

function compararLegacy(ingresada, almacenada) {
  const huellaIngresada = crypto
    .createHash('sha256')
    .update(ingresada, 'utf8')
    .digest();
  const huellaAlmacenada = crypto
    .createHash('sha256')
    .update(almacenada, 'utf8')
    .digest();

  return crypto.timingSafeEqual(huellaIngresada, huellaAlmacenada);
}

async function compararBcrypt(ingresada, hash) {
  try {
    return await bcrypt.compare(ingresada, hash);
  } catch (error) {
    return false;
  }
}

async function verificarContrasena(ingresada, almacenada, algoritmo) {
  if (typeof ingresada !== 'string' || typeof almacenada !== 'string') {
    return false;
  }

  if (algoritmo === ALGORITMO_BCRYPT) {
    const hash = extraerHashEnvuelto(almacenada) || almacenada;
    return compararBcrypt(ingresada, hash);
  }

  if (algoritmo === ALGORITMO_LEGACY) {
    const hashEnvuelto = extraerHashEnvuelto(almacenada);
    if (hashEnvuelto) {
      return compararBcrypt(ingresada, hashEnvuelto);
    }
    return compararLegacy(ingresada, almacenada);
  }

  return false;
}

function necesitaRehash(hash) {
  try {
    return bcrypt.getRounds(hash) < costoBcrypt();
  } catch (error) {
    return false;
  }
}

module.exports = {
  ALGORITMO_LEGACY,
  ALGORITMO_BCRYPT,
  LONGITUD_MINIMA,
  MAXIMO_BYTES_BCRYPT,
  validarNuevaContrasena,
  crearHash,
  crearHashParaMigracion,
  envolverHashParaEsquemaLegacy,
  extraerHashEnvuelto,
  verificarContrasena,
  necesitaRehash
};

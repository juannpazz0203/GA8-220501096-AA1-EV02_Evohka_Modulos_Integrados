const ERROR_CONFIGURACION_RECUPERACION =
  'CONFIGURACION_RECUPERACION_INVALIDA';

const CODIGO_DIGITOS = 8;
const VIGENCIA_CODIGO_MINUTOS = 15;
const MAXIMO_INTENTOS_FALLIDOS = 5;
const MAXIMO_SOLICITUDES_CUENTA = 3;
const VENTANA_LIMITES_MS = 15 * 60 * 1000;
const PEPPER_MINIMO_BYTES = 32;

function errorConfiguracion(mensaje) {
  const error = new Error(mensaje);
  error.code = ERROR_CONFIGURACION_RECUPERACION;
  return error;
}

function recuperacionActiva() {
  return process.env.EVOHKA_RECUPERACION_ACTIVA === 'true';
}

function valorObligatorio(nombre) {
  const valor = process.env[nombre];
  if (typeof valor !== 'string' || valor.trim().length === 0) {
    throw errorConfiguracion(`${nombre} es obligatorio`);
  }
  return valor.trim();
}

function obtenerConfiguracionRecuperacion() {
  if (!recuperacionActiva()) {
    throw errorConfiguracion(
      'La recuperación de contraseña no está habilitada'
    );
  }

  if (process.env.EVOHKA_SESIONES_VERSIONADAS_ACTIVAS !== 'true') {
    throw errorConfiguracion(
      'La recuperación requiere sesiones versionadas activas'
    );
  }

  const pepper = valorObligatorio('EVOHKA_RECUPERACION_PEPPER');
  if (Buffer.byteLength(pepper, 'utf8') < PEPPER_MINIMO_BYTES) {
    throw errorConfiguracion(
      `EVOHKA_RECUPERACION_PEPPER debe tener al menos ${PEPPER_MINIMO_BYTES} bytes UTF-8`
    );
  }

  const puerto = Number(valorObligatorio('EVOHKA_SMTP_PORT'));
  if (!Number.isInteger(puerto) || puerto < 1 || puerto > 65535) {
    throw errorConfiguracion('EVOHKA_SMTP_PORT no es válido');
  }

  const seguroTexto = valorObligatorio('EVOHKA_SMTP_SECURE');
  if (seguroTexto !== 'true' && seguroTexto !== 'false') {
    throw errorConfiguracion(
      'EVOHKA_SMTP_SECURE debe ser true o false'
    );
  }

  return {
    pepper,
    smtp: {
      host: valorObligatorio('EVOHKA_SMTP_HOST'),
      port: puerto,
      secure: seguroTexto === 'true',
      usuario: valorObligatorio('EVOHKA_SMTP_USUARIO'),
      passwordAplicacion: valorObligatorio(
        'EVOHKA_SMTP_PASSWORD_APLICACION'
      ),
      remitente: valorObligatorio('EVOHKA_SMTP_REMITENTE')
    }
  };
}

module.exports = {
  ERROR_CONFIGURACION_RECUPERACION,
  CODIGO_DIGITOS,
  VIGENCIA_CODIGO_MINUTOS,
  MAXIMO_INTENTOS_FALLIDOS,
  MAXIMO_SOLICITUDES_CUENTA,
  VENTANA_LIMITES_MS,
  recuperacionActiva,
  obtenerConfiguracionRecuperacion
};

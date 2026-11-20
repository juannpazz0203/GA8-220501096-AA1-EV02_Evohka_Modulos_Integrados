const LONGITUD_MAXIMA_CORREO = 150;
const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function normalizarCorreo(valor) {
  return typeof valor === 'string'
    ? valor.trim().toLowerCase()
    : '';
}

function correoValido(valor) {
  const correo = normalizarCorreo(valor);
  return correo.length > 0 &&
    correo.length <= LONGITUD_MAXIMA_CORREO &&
    PATRON_CORREO.test(correo);
}

module.exports = {
  LONGITUD_MAXIMA_CORREO,
  normalizarCorreo,
  correoValido
};

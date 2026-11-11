const nodemailer = require('nodemailer');
const {
  obtenerConfiguracionRecuperacion
} = require('../config/recuperacion');

function crearCorreoRecuperacionService({
  obtenerConfiguracion = obtenerConfiguracionRecuperacion,
  crearTransporte = nodemailer.createTransport
} = {}) {
  let transporte;

  function obtenerTransporte() {
    if (transporte) return transporte;

    const { smtp } = obtenerConfiguracion();
    transporte = crearTransporte({
      host: smtp.host,
      port: smtp.port,
      secure: smtp.secure,
      auth: {
        user: smtp.usuario,
        pass: smtp.passwordAplicacion
      }
    });
    return transporte;
  }

  async function enviarCodigo({ destinatario, codigo }) {
    const { smtp } = obtenerConfiguracion();
    return obtenerTransporte().sendMail({
      from: smtp.remitente,
      to: destinatario,
      subject: 'Código de recuperación de EVOHKA',
      text: [
        'Solicitaste restablecer tu contraseña de EVOHKA.',
        `Tu código temporal es: ${codigo}`,
        'El código vence en 15 minutos y solo puede utilizarse una vez.',
        'Si no realizaste esta solicitud, ignora este mensaje.'
      ].join('\n\n')
    });
  }

  return { enviarCodigo };
}

module.exports = {
  crearCorreoRecuperacionService,
  correoRecuperacionService: crearCorreoRecuperacionService()
};

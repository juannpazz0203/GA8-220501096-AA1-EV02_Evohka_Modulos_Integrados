const jwtService = require('../services/jwtService');
const sesionService = require('../services/sesionService');

const verificarToken = async (req, res, next) => {
  const encabezadoAutorizacion = req.headers.authorization;

  if (!encabezadoAutorizacion) {
    return res.status(401).json({
      ok: false,
      mensaje: 'Token no proporcionado'
    });
  }

  const partes = encabezadoAutorizacion.split(' ');

  if (partes.length !== 2 || partes[0] !== 'Bearer') {
    return res.status(401).json({
      ok: false,
      mensaje: 'Formato de token inválido'
    });
  }

  let tokenVerificado;

  try {
    tokenVerificado = jwtService.verificarToken(partes[1]);
  } catch (error) {
    return res.status(401).json({
      ok: false,
      mensaje: 'Token inválido o vencido'
    });
  }

  try {
    const usuario = await sesionService.obtenerUsuarioAutorizacion(
      tokenVerificado.id_usuario
    );

    if (
      !usuario ||
      usuario.sesion_version !== tokenVerificado.sesion_version
    ) {
      return res.status(401).json({
        ok: false,
        mensaje: 'Sesión inválida o revocada'
      });
    }

    req.usuario = usuario;
    req.autenticacion = {
      formatoToken: tokenVerificado.formato
    };

    return next();
  } catch (error) {
    console.error('Error al validar la sesión autenticada:', {
      codigo: error.code || 'ERROR_SESION'
    });
    return res.status(500).json({
      ok: false,
      mensaje: 'No fue posible validar la sesión'
    });
  }
};

module.exports = {
  verificarToken
};

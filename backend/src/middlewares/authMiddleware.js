const jwt = require('jsonwebtoken');

const verificarToken = (req, res, next) => {
  try {
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

    const token = partes[1];

    const usuarioDecodificado = jwt.verify(
      token,
      process.env.JWT_SECRET
    );

    req.usuario = usuarioDecodificado;

    next();
  } catch (error) {
    return res.status(401).json({
      ok: false,
      mensaje: 'Token inválido o vencido'
    });
  }
};

module.exports = {
  verificarToken
};
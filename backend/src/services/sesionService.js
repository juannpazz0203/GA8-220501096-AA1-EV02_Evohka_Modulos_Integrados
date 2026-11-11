const { pool } = require('../config/db');
const {
  sesionesVersionadasActivas
} = require('../config/seguridad');

async function obtenerUsuarioAutorizacion(idUsuario, ejecutor = pool) {
  const incluirVersion = sesionesVersionadasActivas();
  const campoVersion = incluirVersion ? ', sesion_version' : '';
  const [filas] = await ejecutor.execute(
    `SELECT id_usuario, nombre, apellido, correo, telefono, rol${campoVersion}
     FROM usuarios
     WHERE id_usuario = ?
     LIMIT 1`,
    [Number(idUsuario)]
  );
  const usuario = filas[0];

  if (!usuario) return null;

  const sesionVersion = incluirVersion
    ? Number(usuario.sesion_version)
    : 0;

  if (!Number.isSafeInteger(sesionVersion) || sesionVersion < 0) {
    const error = new Error('La versión de sesión almacenada no es válida');
    error.code = 'SESION_VERSION_INVALIDA';
    throw error;
  }

  return {
    ...usuario,
    sesion_version: sesionVersion
  };
}

async function revocarSesionesUsuario(idUsuario, ejecutor = pool) {
  if (!sesionesVersionadasActivas()) {
    return { activada: false, actualizada: false };
  }

  const [resultado] = await ejecutor.execute(
    `UPDATE usuarios
     SET sesion_version = sesion_version + 1
     WHERE id_usuario = ?`,
    [Number(idUsuario)]
  );

  return {
    activada: true,
    actualizada: resultado.affectedRows === 1
  };
}

module.exports = {
  obtenerUsuarioAutorizacion,
  revocarSesionesUsuario
};

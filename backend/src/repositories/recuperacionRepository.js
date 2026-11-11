const { pool } = require('../config/db');

async function conTransaccion(operacion) {
  const conexion = await pool.getConnection();
  try {
    await conexion.beginTransaction();
    const resultado = await operacion(conexion);
    await conexion.commit();
    return resultado;
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

async function buscarUsuarioPorCorreo(correo, ejecutor, bloquear = false) {
  const [filas] = await ejecutor.execute(
    `SELECT id_usuario, correo
     FROM usuarios
     WHERE correo = ?
     LIMIT 1${bloquear ? ' FOR UPDATE' : ''}`,
    [correo]
  );
  return filas[0] || null;
}

async function listarSolicitudesRecientes(idUsuario, desde, ejecutor) {
  const [filas] = await ejecutor.execute(
    `SELECT id_recuperacion
     FROM recuperaciones_contrasena
     WHERE id_usuario = ?
       AND fecha_creacion >= ?
     ORDER BY fecha_creacion DESC, id_recuperacion DESC
     FOR UPDATE`,
    [idUsuario, desde]
  );
  return filas;
}

async function revocarCodigosActivos(
  idUsuario,
  fecha,
  ejecutor,
  idExcluido = null
) {
  const parametros = [fecha, idUsuario];
  const exclusion = idExcluido === null
    ? ''
    : 'AND id_recuperacion <> ?';
  if (idExcluido !== null) parametros.push(idExcluido);

  const [resultado] = await ejecutor.execute(
    `UPDATE recuperaciones_contrasena
     SET fecha_revocacion = ?
     WHERE id_usuario = ?
       AND fecha_uso IS NULL
       AND fecha_revocacion IS NULL
       ${exclusion}`,
    parametros
  );
  return resultado.affectedRows;
}

async function crearCodigo(datos, ejecutor) {
  const [resultado] = await ejecutor.execute(
    `INSERT INTO recuperaciones_contrasena
      (id_usuario, codigo_hash, codigo_salt, fecha_creacion,
       fecha_expiracion, fecha_envio, intentos_fallidos,
       fecha_uso, fecha_revocacion)
     VALUES (?, ?, ?, ?, ?, NULL, 0, NULL, NULL)`,
    [
      datos.idUsuario,
      datos.codigoHash,
      datos.codigoSalt,
      datos.fechaCreacion,
      datos.fechaExpiracion
    ]
  );
  return resultado.insertId;
}

async function marcarCodigoEnviado(idRecuperacion, fechaEnvio) {
  const [resultado] = await pool.execute(
    `UPDATE recuperaciones_contrasena
     SET fecha_envio = ?
     WHERE id_recuperacion = ?
       AND fecha_envio IS NULL
       AND fecha_uso IS NULL
       AND fecha_revocacion IS NULL`,
    [fechaEnvio, idRecuperacion]
  );
  return resultado.affectedRows === 1;
}

async function revocarCodigo(idRecuperacion, fechaRevocacion) {
  const [resultado] = await pool.execute(
    `UPDATE recuperaciones_contrasena
     SET fecha_revocacion = ?
     WHERE id_recuperacion = ?
       AND fecha_uso IS NULL
       AND fecha_revocacion IS NULL`,
    [fechaRevocacion, idRecuperacion]
  );
  return resultado.affectedRows === 1;
}

async function obtenerUltimoCodigo(idUsuario, ejecutor) {
  const [filas] = await ejecutor.execute(
    `SELECT id_recuperacion, codigo_hash, codigo_salt,
       fecha_expiracion, fecha_envio, intentos_fallidos,
       fecha_uso, fecha_revocacion
     FROM recuperaciones_contrasena
     WHERE id_usuario = ?
     ORDER BY fecha_creacion DESC, id_recuperacion DESC
     LIMIT 1
     FOR UPDATE`,
    [idUsuario]
  );
  return filas[0] || null;
}

async function registrarIntentoFallido(
  idRecuperacion,
  intentosFallidos,
  fechaRevocacion,
  ejecutor
) {
  const [resultado] = await ejecutor.execute(
    `UPDATE recuperaciones_contrasena
     SET intentos_fallidos = ?,
       fecha_revocacion = COALESCE(?, fecha_revocacion)
     WHERE id_recuperacion = ?
       AND fecha_uso IS NULL
       AND fecha_revocacion IS NULL`,
    [intentosFallidos, fechaRevocacion, idRecuperacion]
  );
  return resultado.affectedRows === 1;
}

async function actualizarContrasenaYVersion(idUsuario, hash, ejecutor) {
  const [resultado] = await ejecutor.execute(
    `UPDATE usuarios
     SET contrasena = ?,
       contrasena_algoritmo = 'bcrypt',
       sesion_version = sesion_version + 1
     WHERE id_usuario = ?`,
    [hash, idUsuario]
  );
  return resultado.affectedRows === 1;
}

async function marcarCodigoUsado(idRecuperacion, fechaUso, ejecutor) {
  const [resultado] = await ejecutor.execute(
    `UPDATE recuperaciones_contrasena
     SET fecha_uso = ?
     WHERE id_recuperacion = ?
       AND fecha_envio IS NOT NULL
       AND fecha_uso IS NULL
       AND fecha_revocacion IS NULL`,
    [fechaUso, idRecuperacion]
  );
  return resultado.affectedRows === 1;
}

module.exports = {
  conTransaccion,
  buscarUsuarioPorCorreo,
  listarSolicitudesRecientes,
  revocarCodigosActivos,
  crearCodigo,
  marcarCodigoEnviado,
  revocarCodigo,
  obtenerUltimoCodigo,
  registrarIntentoFallido,
  actualizarContrasenaYVersion,
  marcarCodigoUsado
};

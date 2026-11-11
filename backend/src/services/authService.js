const { pool } = require('../config/db');
const contrasenaService = require('./contrasenaService');
const {
  sesionesVersionadasActivas
} = require('../config/seguridad');

async function buscarCredencialPorCorreo(correo) {
  const incluirVersion = sesionesVersionadasActivas();
  const campoVersion = incluirVersion
    ? ', sesion_version'
    : '';
  const [usuarios] = await pool.execute(
    `SELECT
      id_usuario,
      nombre,
      apellido,
      correo,
      contrasena,
      telefono,
      rol,
      contrasena_algoritmo${campoVersion}
     FROM usuarios
     WHERE correo = ?
     LIMIT 1`,
    [correo.trim().toLowerCase()]
  );

  const usuario = usuarios[0];
  if (!usuario) return null;

  return {
    ...usuario,
    sesion_version: incluirVersion
      ? Number(usuario.sesion_version)
      : 0
  };
}

async function verificarCredencialActual(idUsuario, contrasena) {
  const [usuarios] = await pool.execute(
    `SELECT contrasena, contrasena_algoritmo
     FROM usuarios
     WHERE id_usuario = ?
     LIMIT 1`,
    [idUsuario]
  );
  const actual = usuarios[0];
  if (!actual) return false;

  return contrasenaService.verificarContrasena(
    contrasena,
    actual.contrasena,
    actual.contrasena_algoritmo
  );
}

async function migrarCredencialOportunista(usuario, contrasena) {
  const hashEnvuelto = contrasenaService.extraerHashEnvuelto(
    usuario.contrasena
  );
  const hash = hashEnvuelto ||
    await contrasenaService.crearHashParaMigracion(usuario.contrasena);

  // Una credencial legacy mayor a 72 bytes sigue siendo válida, pero requiere
  // un restablecimiento posterior porque bcrypt truncaría su valor.
  if (!hash) return true;

  const [resultado] = await pool.execute(
    `UPDATE usuarios
     SET contrasena = ?, contrasena_algoritmo = ?
     WHERE id_usuario = ?
       AND contrasena = ?
       AND contrasena_algoritmo = ?`,
    [
      hash,
      contrasenaService.ALGORITMO_BCRYPT,
      usuario.id_usuario,
      usuario.contrasena,
      contrasenaService.ALGORITMO_LEGACY
    ]
  );

  if (resultado.affectedRows === 1) return true;

  // Si la fila cambió mientras se calculaba el hash, se valida nuevamente el
  // valor vigente. Así una contraseña antigua no autentica tras un cambio.
  return verificarCredencialActual(usuario.id_usuario, contrasena);
}

async function validarCredenciales(correo, contrasena) {
  const usuario = await buscarCredencialPorCorreo(correo);

  if (!usuario) return null;

  const algoritmo = usuario.contrasena_algoritmo;
  const valida = await contrasenaService.verificarContrasena(
    contrasena,
    usuario.contrasena,
    algoritmo
  );

  if (!valida) return null;

  if (
    algoritmo === contrasenaService.ALGORITMO_LEGACY &&
    !await migrarCredencialOportunista(usuario, contrasena)
  ) {
    return null;
  }

  return {
    id_usuario: usuario.id_usuario,
    nombre: usuario.nombre,
    apellido: usuario.apellido,
    correo: usuario.correo,
    telefono: usuario.telefono,
    rol: usuario.rol,
    sesion_version: usuario.sesion_version
  };
}

async function buscarUsuarioPorCorreo(correo) {
  const [usuarios] = await pool.execute(
    `SELECT id_usuario
     FROM usuarios
     WHERE correo = ?
     LIMIT 1`,
    [correo.trim().toLowerCase()]
  );

  return usuarios.length > 0;
}

async function registrarUsuario(datosUsuario) {
  const {
    nombre,
    apellido,
    correo,
    contrasena,
    telefono,
    rol
  } = datosUsuario;
  const correoNormalizado = correo.trim().toLowerCase();
  const hash = await contrasenaService.crearHash(contrasena);
  const parametros = [
    nombre.trim(),
    apellido.trim(),
    correoNormalizado,
    hash,
    telefono?.trim() || null,
    rol,
    contrasenaService.ALGORITMO_BCRYPT
  ];

  const [resultado] = await pool.execute(
    `INSERT INTO usuarios
      (
        nombre,
        apellido,
        correo,
        contrasena,
        telefono,
        rol,
        fecha_registro,
        contrasena_algoritmo
      )
     VALUES (?, ?, ?, ?, ?, ?, CURDATE(), ?)`,
    parametros
  );

  return {
    id_usuario: resultado.insertId,
    nombre: nombre.trim(),
    apellido: apellido.trim(),
    correo: correoNormalizado,
    telefono: telefono?.trim() || null,
    rol
  };
}

module.exports = {
  validarCredenciales,
  buscarUsuarioPorCorreo,
  registrarUsuario
};

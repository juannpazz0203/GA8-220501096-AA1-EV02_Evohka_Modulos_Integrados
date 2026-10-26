const { pool } = require('../config/db');

const validarCredenciales = async (correo, contrasena) => {
  const [usuarios] = await pool.execute(
    `SELECT
      id_usuario,
      nombre,
      apellido,
      correo,
      contrasena,
      telefono,
      rol
     FROM usuarios
     WHERE correo = ?`,
    [correo.trim().toLowerCase()]
  );

  if (usuarios.length === 0) {
    return null;
  }

  const usuario = usuarios[0];

  if (usuario.contrasena !== contrasena) {
    return null;
  }

  return {
    id_usuario: usuario.id_usuario,
    nombre: usuario.nombre,
    apellido: usuario.apellido,
    correo: usuario.correo,
    telefono: usuario.telefono,
    rol: usuario.rol
  };
};

const buscarUsuarioPorCorreo = async (correo) => {
  const [usuarios] = await pool.execute(
    `SELECT id_usuario
     FROM usuarios
     WHERE correo = ?
     LIMIT 1`,
    [correo.trim().toLowerCase()]
  );

  return usuarios.length > 0;
};

const registrarUsuario = async (datosUsuario) => {
  const {
    nombre,
    apellido,
    correo,
    contrasena,
    telefono,
    rol
  } = datosUsuario;

  const correoNormalizado =
    correo.trim().toLowerCase();

  const [resultado] = await pool.execute(
    `INSERT INTO usuarios
      (
        nombre,
        apellido,
        correo,
        contrasena,
        telefono,
        rol,
        fecha_registro
      )
     VALUES (?, ?, ?, ?, ?, ?, CURDATE())`,
    [
      nombre.trim(),
      apellido.trim(),
      correoNormalizado,
      contrasena,
      telefono?.trim() || null,
      rol
    ]
  );

  return {
    id_usuario: resultado.insertId,
    nombre: nombre.trim(),
    apellido: apellido.trim(),
    correo: correoNormalizado,
    telefono: telefono?.trim() || null,
    rol
  };
};

module.exports = {
  validarCredenciales,
  buscarUsuarioPorCorreo,
  registrarUsuario
};
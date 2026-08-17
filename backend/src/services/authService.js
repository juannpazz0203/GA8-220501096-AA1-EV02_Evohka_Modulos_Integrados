const { pool } = require('../config/db');

const validarCredenciales = async (correo, contrasena) => {
  try {
    const [usuarios] = await pool.execute(
      `SELECT
        id_usuario,
        nombre,
        apellido,
        correo,
        contrasena,
        rol
       FROM usuarios
       WHERE correo = ?`,
      [correo]
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
      rol: usuario.rol
    };
  } catch (error) {
    throw error;
  }
};

module.exports = {
  validarCredenciales
};
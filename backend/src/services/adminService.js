const { pool } = require('../config/db');

const SELECCION_USUARIO = `SELECT
  id_usuario,
  nombre,
  apellido,
  correo,
  telefono,
  rol,
  fecha_registro
 FROM usuarios`;

async function obtenerUsuarioPorId(idUsuario, ejecutor = pool) {
  const [filas] = await ejecutor.execute(
    `${SELECCION_USUARIO}
     WHERE id_usuario = ?
     LIMIT 1`,
    [Number(idUsuario)]
  );
  return filas[0] || null;
}

async function listarUsuarios() {
  const [filas] = await pool.execute(
    `${SELECCION_USUARIO}
     ORDER BY fecha_registro DESC, id_usuario DESC`
  );
  return filas;
}

async function correoEnUso(correo, idExcluido = null, ejecutor = pool) {
  const parametros = [correo.trim().toLowerCase()];
  let exclusion = '';

  if (idExcluido) {
    exclusion = 'AND id_usuario <> ?';
    parametros.push(Number(idExcluido));
  }

  const [filas] = await ejecutor.execute(
    `SELECT id_usuario
     FROM usuarios
     WHERE correo = ?
       ${exclusion}
     LIMIT 1`,
    parametros
  );
  return filas.length > 0;
}

async function crearUsuario(datos) {
  const correo = datos.correo.trim().toLowerCase();
  if (await correoEnUso(correo)) {
    return { error: 'correo_duplicado' };
  }

  const [resultado] = await pool.execute(
    `INSERT INTO usuarios
      (nombre, apellido, correo, contrasena, telefono, rol, fecha_registro)
     VALUES (?, ?, ?, ?, ?, ?, CURDATE())`,
    [
      datos.nombre.trim(),
      datos.apellido.trim(),
      correo,
      datos.contrasena,
      datos.telefono,
      datos.rol
    ]
  );

  return {
    datos: await obtenerUsuarioPorId(resultado.insertId)
  };
}

async function actualizarUsuario(idUsuario, idAdministrador, datos) {
  const conexion = await pool.getConnection();

  try {
    await conexion.beginTransaction();
    const [filas] = await conexion.execute(
      `SELECT id_usuario, rol
       FROM usuarios
       WHERE id_usuario = ?
       LIMIT 1
       FOR UPDATE`,
      [Number(idUsuario)]
    );
    const usuario = filas[0];

    if (!usuario) {
      await conexion.rollback();
      return { error: 'no_existe' };
    }

    if (
      Number(idUsuario) === Number(idAdministrador) &&
      String(usuario.rol).toLowerCase() !== datos.rol
    ) {
      await conexion.rollback();
      return { error: 'auto_cambio_rol' };
    }

    if (
      String(usuario.rol).toLowerCase() === 'administrador' &&
      datos.rol !== 'administrador'
    ) {
      const [administradores] = await conexion.execute(
        `SELECT id_usuario
         FROM usuarios
         WHERE LOWER(rol) = 'administrador'
         FOR UPDATE`
      );
      if (administradores.length <= 1) {
        await conexion.rollback();
        return { error: 'ultimo_administrador' };
      }
    }

    if (await correoEnUso(datos.correo, idUsuario, conexion)) {
      await conexion.rollback();
      return { error: 'correo_duplicado' };
    }

    await conexion.execute(
      `UPDATE usuarios
       SET nombre = ?, apellido = ?, correo = ?, telefono = ?, rol = ?
       WHERE id_usuario = ?`,
      [
        datos.nombre.trim(),
        datos.apellido.trim(),
        datos.correo.trim().toLowerCase(),
        datos.telefono,
        datos.rol,
        Number(idUsuario)
      ]
    );

    const actualizado = await obtenerUsuarioPorId(idUsuario, conexion);
    await conexion.commit();
    return { datos: actualizado };
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

async function tieneRelaciones(idUsuario, ejecutor) {
  const parametros = Array(7).fill(Number(idUsuario));
  const [filas] = await ejecutor.execute(
    `SELECT
      EXISTS(SELECT 1 FROM propiedades WHERE id_propietario = ?) AS propiedades,
      EXISTS(SELECT 1 FROM solicitudes WHERE id_usuario = ?) AS solicitudes,
      EXISTS(SELECT 1 FROM reportes WHERE id_usuario = ?) AS reportes,
      EXISTS(SELECT 1 FROM mensajes WHERE id_remitente = ?) AS mensajes_enviados,
      EXISTS(SELECT 1 FROM mensajes WHERE id_destinatario = ?) AS mensajes_recibidos,
      EXISTS(SELECT 1 FROM notificaciones WHERE id_usuario = ?) AS notificaciones,
      EXISTS(
        SELECT 1
        FROM contratos AS c
        INNER JOIN solicitudes AS s ON s.id_solicitud = c.id_solicitud
        WHERE s.id_usuario = ?
      ) AS contratos`,
    parametros
  );

  return Object.values(filas[0] || {}).some((valor) => Number(valor) === 1);
}

async function eliminarUsuario(idUsuario, idAdministrador) {
  const conexion = await pool.getConnection();

  try {
    await conexion.beginTransaction();
    const [filas] = await conexion.execute(
      `SELECT id_usuario, nombre, apellido, correo, telefono, rol, fecha_registro
       FROM usuarios
       WHERE id_usuario = ?
       LIMIT 1
       FOR UPDATE`,
      [Number(idUsuario)]
    );
    const usuario = filas[0];

    if (!usuario) {
      await conexion.rollback();
      return { error: 'no_existe' };
    }

    if (Number(idUsuario) === Number(idAdministrador)) {
      await conexion.rollback();
      return { error: 'auto_eliminacion' };
    }

    if (String(usuario.rol).toLowerCase() === 'administrador') {
      const [administradores] = await conexion.execute(
        `SELECT id_usuario
         FROM usuarios
         WHERE LOWER(rol) = 'administrador'
         FOR UPDATE`
      );
      if (administradores.length <= 1) {
        await conexion.rollback();
        return { error: 'ultimo_administrador' };
      }
    }

    if (await tieneRelaciones(idUsuario, conexion)) {
      await conexion.rollback();
      return { error: 'tiene_relaciones' };
    }

    await conexion.execute(
      'DELETE FROM usuarios WHERE id_usuario = ?',
      [Number(idUsuario)]
    );
    await conexion.commit();
    return { datos: usuario };
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

async function contar(tabla) {
  const tablasPermitidas = new Set([
    'usuarios', 'propiedades', 'solicitudes', 'contratos',
    'pagos', 'reportes', 'mensajes', 'notificaciones'
  ]);
  if (!tablasPermitidas.has(tabla)) throw new Error('Tabla no permitida');
  const [filas] = await pool.query(`SELECT COUNT(*) AS total FROM ${tabla}`);
  return Number(filas[0]?.total || 0);
}

async function obtenerResumen() {
  const nombres = [
    'usuarios', 'propiedades', 'solicitudes', 'contratos',
    'pagos', 'reportes', 'mensajes', 'notificaciones'
  ];
  const totalesValores = await Promise.all(nombres.map(contar));
  const totales = Object.fromEntries(
    nombres.map((nombre, indice) => [
      nombre === 'reportes' ? 'fallas' : nombre,
      totalesValores[indice]
    ])
  );

  const [usuarios] = await pool.execute(
    `SELECT id_usuario, nombre, apellido, correo, telefono, rol, fecha_registro
     FROM usuarios
     ORDER BY fecha_registro DESC, id_usuario DESC
     LIMIT 50`
  );
  const [propiedades] = await pool.execute(
    `SELECT p.id_propiedad, p.titulo, p.tipo_inmueble, p.direccion,
      p.ciudad, p.canon, p.id_propietario,
      u.nombre AS nombre_propietario, u.apellido AS apellido_propietario
     FROM propiedades AS p
     INNER JOIN usuarios AS u ON u.id_usuario = p.id_propietario
     ORDER BY p.id_propiedad DESC
     LIMIT 50`
  );
  const [solicitudes] = await pool.execute(
    `SELECT s.id_solicitud, s.fecha_solicitud, s.estado,
      s.id_usuario, s.id_propiedad, p.titulo AS titulo_propiedad
     FROM solicitudes AS s
     INNER JOIN propiedades AS p ON p.id_propiedad = s.id_propiedad
     ORDER BY s.fecha_solicitud DESC, s.id_solicitud DESC
     LIMIT 50`
  );
  const [contratos] = await pool.execute(
    `SELECT c.id_contrato, c.id_solicitud, c.fecha_inicio, c.fecha_fin,
      c.valor_mensual, c.estado, s.id_usuario AS id_arrendatario,
      s.id_propiedad, p.titulo AS titulo_propiedad, p.id_propietario
     FROM contratos AS c
     INNER JOIN solicitudes AS s ON s.id_solicitud = c.id_solicitud
     INNER JOIN propiedades AS p ON p.id_propiedad = s.id_propiedad
     ORDER BY c.id_contrato DESC
     LIMIT 50`
  );
  const [fallas] = await pool.execute(
    `SELECT r.id_reporte AS id_falla, r.id_usuario, r.id_propiedad,
      p.titulo AS titulo_propiedad, r.categoria, r.descripcion,
      r.fecha_reporte, r.estado
     FROM reportes AS r
     INNER JOIN propiedades AS p ON p.id_propiedad = r.id_propiedad
     ORDER BY r.fecha_reporte DESC, r.id_reporte DESC
     LIMIT 50`
  );

  return { totales, usuarios, propiedades, solicitudes, contratos, fallas };
}

module.exports = {
  obtenerResumen,
  listarUsuarios,
  crearUsuario,
  actualizarUsuario,
  eliminarUsuario
};

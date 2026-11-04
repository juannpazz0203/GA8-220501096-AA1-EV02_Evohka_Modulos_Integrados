const { pool } = require('../config/db');
const notificacionService = require('./notificacionService');

async function obtenerContratoPorId(
  idContrato,
  ejecutor = pool
) {
  const [filas] = await ejecutor.execute(
    `SELECT
      c.id_contrato,
      c.id_solicitud,
      c.fecha_inicio,
      c.fecha_fin,
      c.valor_mensual,
      c.deposito,
      c.estado,
      c.descripcion,
      s.id_usuario AS id_arrendatario,
      u.nombre AS nombre_arrendatario,
      u.apellido AS apellido_arrendatario,
      s.id_propiedad,
      p.titulo AS titulo_propiedad,
      p.id_propietario
     FROM contratos AS c
     INNER JOIN solicitudes AS s
       ON s.id_solicitud = c.id_solicitud
     INNER JOIN usuarios AS u
       ON u.id_usuario = s.id_usuario
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     WHERE c.id_contrato = ?
     LIMIT 1`,
    [Number(idContrato)]
  );

  return filas[0] || null;
}

async function crearContrato(datos) {
  const conexion = await pool.getConnection();

  try {
    await conexion.beginTransaction();

    const [solicitudes] = await conexion.execute(
      `SELECT
        s.id_solicitud,
        s.id_usuario AS id_arrendatario,
        s.estado AS estado_solicitud,
        p.id_propietario,
        p.titulo AS titulo_propiedad
       FROM solicitudes AS s
       INNER JOIN propiedades AS p
         ON p.id_propiedad = s.id_propiedad
       WHERE s.id_solicitud = ?
       LIMIT 1
       FOR UPDATE`,
      [Number(datos.id_solicitud)]
    );

    const solicitud = solicitudes[0];

    if (!solicitud) {
      await conexion.rollback();
      return { error: 'solicitud_no_existe' };
    }

    if (
      Number(solicitud.id_propietario) !==
      Number(datos.id_propietario)
    ) {
      await conexion.rollback();
      return { error: 'sin_permiso' };
    }

    if (
      String(solicitud.estado_solicitud).toLowerCase() !==
      'aprobada'
    ) {
      await conexion.rollback();
      return { error: 'solicitud_no_aprobada' };
    }

    const [contratosActivos] = await conexion.execute(
      `SELECT id_contrato
       FROM contratos
       WHERE id_solicitud = ?
         AND LOWER(estado) = 'activo'
       LIMIT 1`,
      [Number(datos.id_solicitud)]
    );

    if (contratosActivos.length > 0) {
      await conexion.rollback();
      return { error: 'contrato_activo_existente' };
    }

    const [resultado] = await conexion.execute(
      `INSERT INTO contratos
        (
          id_solicitud,
          fecha_inicio,
          fecha_fin,
          valor_mensual,
          deposito,
          estado,
          descripcion
        )
       VALUES (?, ?, ?, ?, ?, 'activo', ?)`,
      [
        Number(datos.id_solicitud),
        datos.fecha_inicio,
        datos.fecha_fin,
        Number(datos.valor_mensual),
        datos.deposito,
        datos.descripcion
      ]
    );

    await notificacionService.crearNotificacion(
      conexion,
      solicitud.id_arrendatario,
      'Nuevo contrato',
      `Se creó el contrato de ${solicitud.titulo_propiedad}.`
    );

    const contratoCreado =
      await obtenerContratoPorId(
        resultado.insertId,
        conexion
      );

    await conexion.commit();

    return {
      datos: contratoCreado
    };
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

async function listarContratosUsuario(
  idUsuario,
  rol
) {
  const condicion =
    rol === 'propietario'
      ? 'p.id_propietario = ?'
      : 's.id_usuario = ?';

  const [filas] = await pool.execute(
    `SELECT
      c.id_contrato,
      c.id_solicitud,
      c.fecha_inicio,
      c.fecha_fin,
      c.valor_mensual,
      c.deposito,
      c.estado,
      c.descripcion,
      s.id_usuario AS id_arrendatario,
      u.nombre AS nombre_arrendatario,
      u.apellido AS apellido_arrendatario,
      s.id_propiedad,
      p.titulo AS titulo_propiedad,
      p.id_propietario
     FROM contratos AS c
     INNER JOIN solicitudes AS s
       ON s.id_solicitud = c.id_solicitud
     INNER JOIN usuarios AS u
       ON u.id_usuario = s.id_usuario
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     WHERE ${condicion}
     ORDER BY
       CASE WHEN LOWER(c.estado) = 'activo' THEN 0 ELSE 1 END,
       c.fecha_inicio DESC,
       c.id_contrato DESC`,
    [Number(idUsuario)]
  );

  return filas;
}

async function actualizarEstadoContrato(
  idContrato,
  idPropietario,
  estado
) {
  const conexion = await pool.getConnection();

  try {
    await conexion.beginTransaction();

    const [filas] = await conexion.execute(
      `SELECT
        c.id_contrato,
        c.estado,
        s.id_usuario AS id_arrendatario,
        p.id_propietario,
        p.titulo AS titulo_propiedad
       FROM contratos AS c
       INNER JOIN solicitudes AS s
         ON s.id_solicitud = c.id_solicitud
       INNER JOIN propiedades AS p
         ON p.id_propiedad = s.id_propiedad
       WHERE c.id_contrato = ?
       LIMIT 1
       FOR UPDATE`,
      [Number(idContrato)]
    );

    const contrato = filas[0];

    if (!contrato) {
      await conexion.rollback();
      return { error: 'contrato_no_existe' };
    }

    if (
      Number(contrato.id_propietario) !==
      Number(idPropietario)
    ) {
      await conexion.rollback();
      return { error: 'sin_permiso' };
    }

    if (String(contrato.estado).toLowerCase() !== 'activo') {
      await conexion.rollback();
      return { error: 'contrato_no_activo' };
    }

    await conexion.execute(
      `UPDATE contratos
       SET estado = ?
       WHERE id_contrato = ?`,
      [
        estado,
        Number(idContrato)
      ]
    );

    await notificacionService.crearNotificacion(
      conexion,
      contrato.id_arrendatario,
      'Contrato actualizado',
      `El contrato de ${contrato.titulo_propiedad} fue marcado como ${estado}.`
    );

    const contratoActualizado =
      await obtenerContratoPorId(
        idContrato,
        conexion
      );

    await conexion.commit();

    return {
      datos: contratoActualizado
    };
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

module.exports = {
  obtenerContratoPorId,
  crearContrato,
  listarContratosUsuario,
  actualizarEstadoContrato
};

const { pool } = require('../config/db');
const notificacionService = require('./notificacionService');

function usuarioParticipa(
  contrato,
  idUsuario,
  rol
) {
  if (rol === 'propietario') {
    return (
      Number(contrato.id_propietario) ===
      Number(idUsuario)
    );
  }

  if (rol === 'arrendatario') {
    return (
      Number(contrato.id_arrendatario) ===
      Number(idUsuario)
    );
  }

  return false;
}

async function obtenerContratoParaPago(
  idContrato,
  ejecutor = pool
) {
  const [filas] = await ejecutor.execute(
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
     LIMIT 1`,
    [Number(idContrato)]
  );

  return filas[0] || null;
}

async function obtenerPagoPorId(
  idPago,
  ejecutor = pool
) {
  const [filas] = await ejecutor.execute(
    `SELECT
      pg.id_pago,
      pg.id_contrato,
      pg.fecha_pago,
      pg.monto,
      pg.metodo_pago,
      pg.estado,
      pg.observaciones,
      s.id_usuario AS id_arrendatario,
      p.id_propiedad,
      p.titulo AS titulo_propiedad,
      p.id_propietario
     FROM pagos AS pg
     INNER JOIN contratos AS c
       ON c.id_contrato = pg.id_contrato
     INNER JOIN solicitudes AS s
       ON s.id_solicitud = c.id_solicitud
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     WHERE pg.id_pago = ?
     LIMIT 1`,
    [Number(idPago)]
  );

  return filas[0] || null;
}

async function crearPago(datos) {
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
      [Number(datos.id_contrato)]
    );

    const contrato = filas[0];

    if (!contrato) {
      await conexion.rollback();
      return { error: 'contrato_no_existe' };
    }

    if (
      !usuarioParticipa(
        contrato,
        datos.id_usuario,
        datos.rol
      )
    ) {
      await conexion.rollback();
      return { error: 'sin_permiso' };
    }

    if (String(contrato.estado).toLowerCase() !== 'activo') {
      await conexion.rollback();
      return { error: 'contrato_no_activo' };
    }

    const [resultado] = await conexion.execute(
      `INSERT INTO pagos
        (
          id_contrato,
          fecha_pago,
          monto,
          metodo_pago,
          estado,
          observaciones
        )
       VALUES (?, ?, ?, ?, 'registrado', ?)`,
      [
        Number(datos.id_contrato),
        datos.fecha_pago,
        Number(datos.monto),
        datos.metodo_pago,
        datos.observaciones
      ]
    );

    const idDestinatario =
      datos.rol === 'arrendatario'
        ? contrato.id_propietario
        : contrato.id_arrendatario;

    await notificacionService.crearNotificacion(
      conexion,
      idDestinatario,
      'Pago registrado',
      `Se registró un pago del contrato de ${contrato.titulo_propiedad}.`
    );

    const pagoCreado = await obtenerPagoPorId(
      resultado.insertId,
      conexion
    );

    await conexion.commit();

    return {
      datos: pagoCreado
    };
  } catch (error) {
    await conexion.rollback();
    throw error;
  } finally {
    conexion.release();
  }
}

async function listarPagosUsuario(
  idUsuario,
  rol,
  idContrato
) {
  const condicion =
    rol === 'propietario'
      ? 'p.id_propietario = ?'
      : 's.id_usuario = ?';

  const parametros = [Number(idUsuario)];
  let filtroContrato = '';

  if (idContrato) {
    filtroContrato = 'AND pg.id_contrato = ?';
    parametros.push(Number(idContrato));
  }

  const [filas] = await pool.execute(
    `SELECT
      pg.id_pago,
      pg.id_contrato,
      pg.fecha_pago,
      pg.monto,
      pg.metodo_pago,
      pg.estado,
      pg.observaciones,
      s.id_usuario AS id_arrendatario,
      p.id_propiedad,
      p.titulo AS titulo_propiedad,
      p.id_propietario
     FROM pagos AS pg
     INNER JOIN contratos AS c
       ON c.id_contrato = pg.id_contrato
     INNER JOIN solicitudes AS s
       ON s.id_solicitud = c.id_solicitud
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     WHERE ${condicion}
       ${filtroContrato}
     ORDER BY pg.fecha_pago DESC, pg.id_pago DESC`,
    parametros
  );

  return filas;
}

module.exports = {
  usuarioParticipa,
  obtenerContratoParaPago,
  crearPago,
  listarPagosUsuario
};

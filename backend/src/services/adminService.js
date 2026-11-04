const { pool } = require('../config/db');

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

module.exports = { obtenerResumen };

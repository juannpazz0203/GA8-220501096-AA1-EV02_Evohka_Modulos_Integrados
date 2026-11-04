const { pool } = require('../config/db');

async function obtenerReporteIngresos(idPropietario) {
  const [resumenFilas] = await pool.execute(
    `SELECT
      COUNT(pg.id_pago) AS cantidad_pagos,
      COALESCE(SUM(pg.monto), 0) AS total_ingresos
     FROM pagos AS pg
     INNER JOIN contratos AS c
       ON c.id_contrato = pg.id_contrato
     INNER JOIN solicitudes AS s
       ON s.id_solicitud = c.id_solicitud
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     WHERE p.id_propietario = ?`,
    [Number(idPropietario)]
  );

  const [porPropiedadFilas] = await pool.execute(
    `SELECT
      p.id_propiedad,
      p.titulo AS titulo_propiedad,
      COUNT(pg.id_pago) AS cantidad_pagos,
      COALESCE(SUM(pg.monto), 0) AS total_ingresos
     FROM pagos AS pg
     INNER JOIN contratos AS c
       ON c.id_contrato = pg.id_contrato
     INNER JOIN solicitudes AS s
       ON s.id_solicitud = c.id_solicitud
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     WHERE p.id_propietario = ?
     GROUP BY p.id_propiedad, p.titulo
     ORDER BY total_ingresos DESC, p.titulo ASC`,
    [Number(idPropietario)]
  );

  const [detalleFilas] = await pool.execute(
    `SELECT
      pg.id_pago,
      pg.id_contrato,
      pg.fecha_pago,
      pg.monto,
      pg.metodo_pago,
      pg.estado,
      p.id_propiedad,
      p.titulo AS titulo_propiedad
     FROM pagos AS pg
     INNER JOIN contratos AS c
       ON c.id_contrato = pg.id_contrato
     INNER JOIN solicitudes AS s
       ON s.id_solicitud = c.id_solicitud
     INNER JOIN propiedades AS p
       ON p.id_propiedad = s.id_propiedad
     WHERE p.id_propietario = ?
     ORDER BY pg.fecha_pago DESC, pg.id_pago DESC`,
    [Number(idPropietario)]
  );

  const resumen = resumenFilas[0] || {};

  return {
    resumen: {
      cantidad_pagos: Number(
        resumen.cantidad_pagos || 0
      ),
      total_ingresos: Number(
        resumen.total_ingresos || 0
      )
    },
    por_propiedad: porPropiedadFilas.map(
      (fila) => ({
        ...fila,
        cantidad_pagos: Number(
          fila.cantidad_pagos || 0
        ),
        total_ingresos: Number(
          fila.total_ingresos || 0
        )
      })
    ),
    detalle: detalleFilas
  };
}

module.exports = {
  obtenerReporteIngresos
};

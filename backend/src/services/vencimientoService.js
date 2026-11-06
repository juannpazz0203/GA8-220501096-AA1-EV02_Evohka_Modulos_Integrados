const { pool } = require('../config/db');
const notificacionService = require('./notificacionService');

function esBisiesto(anio) {
  return anio % 4 === 0 && (anio % 100 !== 0 || anio % 400 === 0);
}

function diasEnMes(anio, mes) {
  const dias = [31, esBisiesto(anio) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return dias[mes - 1] || 0;
}

function analizarFecha(valor) {
  if (typeof valor !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
    throw new Error(`Fecha de calendario no válida: ${valor}`);
  }

  const [anio, mes, dia] = valor.split('-').map(Number);
  if (mes < 1 || mes > 12 || dia < 1 || dia > diasEnMes(anio, mes)) {
    throw new Error(`Fecha de calendario no válida: ${valor}`);
  }

  return { anio, mes, dia };
}

function formatearFecha({ anio, mes, dia }) {
  return `${String(anio).padStart(4, '0')}-${String(mes).padStart(2, '0')}-${String(dia).padStart(2, '0')}`;
}

function fechaLegible(valor) {
  const fecha = analizarFecha(valor);
  return `${String(fecha.dia).padStart(2, '0')}/${String(fecha.mes).padStart(2, '0')}/${fecha.anio}`;
}

// Convierte una fecha civil a un número de día sin depender de zona horaria.
function numeroDiaCivil(valor) {
  const fecha = typeof valor === 'string' ? analizarFecha(valor) : valor;
  let anio = fecha.anio;
  const mes = fecha.mes;
  const dia = fecha.dia;

  anio -= mes <= 2 ? 1 : 0;
  const era = Math.floor(anio / 400);
  const anioEra = anio - era * 400;
  const mesAjustado = mes + (mes > 2 ? -3 : 9);
  const diaAnio = Math.floor((153 * mesAjustado + 2) / 5) + dia - 1;
  const diaEra = anioEra * 365 + Math.floor(anioEra / 4) -
    Math.floor(anioEra / 100) + diaAnio;

  return era * 146097 + diaEra;
}

function diferenciaDias(desde, hasta) {
  return numeroDiaCivil(hasta) - numeroDiaCivil(desde);
}

function calcularVencimientoMensual(fechaInicio, fechaReferencia) {
  const inicio = analizarFecha(fechaInicio);
  const referencia = analizarFecha(fechaReferencia);
  return calcularVencimientoParaMes(
    inicio,
    referencia.anio,
    referencia.mes
  );
}

function calcularVencimientoParaMes(inicio, anio, mes) {
  const dia = Math.min(
    inicio.dia,
    diasEnMes(anio, mes)
  );

  return formatearFecha({
    anio,
    mes,
    dia
  });
}

function datosBaseMensaje(contrato) {
  return `${contrato.titulo_propiedad} · Contrato #${contrato.id_contrato}`;
}

function crearAvisoContrato(contrato, fechaActual) {
  if (String(contrato.estado || 'activo').toLowerCase() !== 'activo') return null;
  const dias = diferenciaDias(fechaActual, contrato.fecha_fin);
  if (dias !== 7 && dias !== 0) return null;

  const titulo = dias === 7
    ? 'Tu contrato vence en 7 días'
    : 'Tu contrato vence hoy';

  return {
    tipo: dias === 7 ? 'contrato_7_dias' : 'contrato_hoy',
    titulo,
    mensaje: `${datosBaseMensaje(contrato)} · vence ${fechaLegible(contrato.fecha_fin)}.`,
    fecha_objetivo: contrato.fecha_fin
  };
}

function crearAvisoPago(contrato, fechaActual) {
  if (String(contrato.estado || 'activo').toLowerCase() !== 'activo') return null;
  const inicio = analizarFecha(contrato.fecha_inicio);
  const referencia = analizarFecha(fechaActual);
  const siguiente = referencia.mes === 12
    ? { anio: referencia.anio + 1, mes: 1 }
    : { anio: referencia.anio, mes: referencia.mes + 1 };
  const candidatos = [
    calcularVencimientoParaMes(inicio, referencia.anio, referencia.mes),
    calcularVencimientoParaMes(inicio, siguiente.anio, siguiente.mes)
  ];

  const fechaVencimiento = candidatos.find((fecha) => {
    if (
      diferenciaDias(contrato.fecha_inicio, fecha) < 0 ||
      diferenciaDias(fecha, contrato.fecha_fin) < 0
    ) {
      return false;
    }
    const dias = diferenciaDias(fechaActual, fecha);
    return dias === 3 || dias === 0;
  });

  if (!fechaVencimiento) return null;
  const dias = diferenciaDias(fechaActual, fechaVencimiento);

  const ciclo = fechaVencimiento.slice(0, 7);
  const titulo = dias === 3
    ? 'Tu pago vence en 3 días'
    : 'Tu pago vence hoy';

  return {
    tipo: dias === 3 ? 'pago_3_dias' : 'pago_hoy',
    titulo,
    mensaje: `${datosBaseMensaje(contrato)} · ciclo ${ciclo} · vence ${fechaLegible(fechaVencimiento)}.`,
    fecha_objetivo: fechaVencimiento,
    ciclo
  };
}

async function cicloPagado(ejecutor, idContrato, ciclo) {
  const [filas] = await ejecutor.execute(
    `SELECT id_pago
     FROM pagos
     WHERE id_contrato = ?
       AND LOWER(estado) = 'registrado'
       AND DATE_FORMAT(fecha_pago, '%Y-%m') = ?
     LIMIT 1`,
    [Number(idContrato), ciclo]
  );

  return filas.length > 0;
}

async function notificarParticipantes(ejecutor, contrato, aviso) {
  const participantes = new Set([
    Number(contrato.id_propietario),
    Number(contrato.id_arrendatario)
  ]);
  let creadas = 0;

  for (const idUsuario of participantes) {
    if (idUsuario <= 0) continue;
    const creada = await notificacionService.crearNotificacionSiNoExiste(
      ejecutor,
      idUsuario,
      aviso.titulo,
      aviso.mensaje
    );
    if (creada) creadas += 1;
  }

  return creadas;
}

async function revisarVencimientosUsuario(idUsuario) {
  const conexion = await pool.getConnection();

  try {
    const [fechas] = await conexion.execute(
      `SELECT DATE_FORMAT(CURDATE(), '%Y-%m-%d') AS fecha_actual`
    );
    const fechaActual = fechas[0]?.fecha_actual;
    analizarFecha(fechaActual);

    const [contratos] = await conexion.execute(
      `SELECT
        c.id_contrato,
        c.estado,
        DATE_FORMAT(c.fecha_inicio, '%Y-%m-%d') AS fecha_inicio,
        DATE_FORMAT(c.fecha_fin, '%Y-%m-%d') AS fecha_fin,
        s.id_usuario AS id_arrendatario,
        p.id_propietario,
        p.titulo AS titulo_propiedad
       FROM contratos AS c
       INNER JOIN solicitudes AS s ON s.id_solicitud = c.id_solicitud
       INNER JOIN propiedades AS p ON p.id_propiedad = s.id_propiedad
       WHERE LOWER(c.estado) = 'activo'
         AND (s.id_usuario = ? OR p.id_propietario = ?)`,
      [Number(idUsuario), Number(idUsuario)]
    );

    let creadas = 0;

    for (const contrato of contratos) {
      const avisoContrato = crearAvisoContrato(contrato, fechaActual);
      if (avisoContrato) {
        creadas += await notificarParticipantes(
          conexion,
          contrato,
          avisoContrato
        );
      }

      const avisoPago = crearAvisoPago(contrato, fechaActual);
      if (
        avisoPago &&
        !await cicloPagado(conexion, contrato.id_contrato, avisoPago.ciclo)
      ) {
        creadas += await notificarParticipantes(
          conexion,
          contrato,
          avisoPago
        );
      }
    }

    return { creadas };
  } finally {
    conexion.release();
  }
}

module.exports = {
  esBisiesto,
  diasEnMes,
  analizarFecha,
  diferenciaDias,
  calcularVencimientoMensual,
  crearAvisoContrato,
  crearAvisoPago,
  cicloPagado,
  revisarVencimientosUsuario
};

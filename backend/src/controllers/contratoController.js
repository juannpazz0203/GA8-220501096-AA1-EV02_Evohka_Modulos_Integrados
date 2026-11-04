const contratoService = require('../services/contratoService');

const ESTADOS_CIERRE = [
  'finalizado',
  'cancelado'
];

function obtenerId(valor) {
  const id = Number(valor);

  return Number.isInteger(id) && id > 0
    ? id
    : null;
}

function esFechaValida(valor) {
  if (
    typeof valor !== 'string' ||
    !/^\d{4}-\d{2}-\d{2}$/.test(valor)
  ) {
    return false;
  }

  const [anio, mes, dia] = valor
    .split('-')
    .map(Number);

  const fecha = new Date(
    Date.UTC(anio, mes - 1, dia)
  );

  return (
    fecha.getUTCFullYear() === anio &&
    fecha.getUTCMonth() === mes - 1 &&
    fecha.getUTCDate() === dia
  );
}

function validarContrato(datos = {}) {
  const idSolicitud = obtenerId(
    datos.id_solicitud
  );

  if (!idSolicitud) {
    return {
      error: 'El identificador de la solicitud no es válido'
    };
  }

  if (
    !esFechaValida(datos.fecha_inicio) ||
    !esFechaValida(datos.fecha_fin)
  ) {
    return {
      error: 'Las fechas deben tener el formato AAAA-MM-DD'
    };
  }

  if (datos.fecha_fin <= datos.fecha_inicio) {
    return {
      error: 'La fecha de finalización debe ser posterior a la fecha de inicio'
    };
  }

  const valorMensual = Number(
    datos.valor_mensual
  );

  if (
    !Number.isFinite(valorMensual) ||
    valorMensual <= 0 ||
    valorMensual > 99999999.99
  ) {
    return {
      error: 'El valor mensual no es válido'
    };
  }

  let deposito = null;

  if (
    datos.deposito !== undefined &&
    datos.deposito !== null &&
    datos.deposito !== ''
  ) {
    deposito = Number(datos.deposito);

    if (
      !Number.isFinite(deposito) ||
      deposito < 0 ||
      deposito > 99999999.99
    ) {
      return {
        error: 'El depósito no es válido'
      };
    }
  }

  const descripcion =
    typeof datos.descripcion === 'string'
      ? datos.descripcion.trim()
      : '';

  if (descripcion.length > 55) {
    return {
      error: 'La descripción no puede superar 55 caracteres'
    };
  }

  return {
    datos: {
      id_solicitud: idSolicitud,
      fecha_inicio: datos.fecha_inicio,
      fecha_fin: datos.fecha_fin,
      valor_mensual: valorMensual,
      deposito,
      descripcion: descripcion || null
    }
  };
}

function responderErrorCreacion(
  res,
  error
) {
  const respuestas = {
    solicitud_no_existe: [
      404,
      'La solicitud no existe'
    ],
    sin_permiso: [
      403,
      'No tiene permiso para crear un contrato con esta solicitud'
    ],
    solicitud_no_aprobada: [
      409,
      'Solo se puede crear un contrato desde una solicitud aprobada'
    ],
    contrato_activo_existente: [
      409,
      'Ya existe un contrato activo para esta solicitud'
    ]
  };

  const [codigo, mensaje] =
    respuestas[error] || [
      409,
      'No fue posible crear el contrato'
    ];

  return res.status(codigo).json({ mensaje });
}

async function crearContrato(req, res) {
  try {
    if (req.usuario?.rol !== 'propietario') {
      return res.status(403).json({
        mensaje: 'Solo los propietarios pueden crear contratos'
      });
    }

    const validacion = validarContrato(req.body);

    if (validacion.error) {
      return res.status(400).json({
        mensaje: validacion.error
      });
    }

    const resultado = await contratoService.crearContrato({
      ...validacion.datos,
      id_propietario: req.usuario.id_usuario
    });

    if (resultado.error) {
      return responderErrorCreacion(
        res,
        resultado.error
      );
    }

    return res.status(201).json({
      mensaje: 'Contrato creado correctamente',
      datos: resultado.datos
    });
  } catch (error) {
    console.error(
      'Error al crear contrato:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible crear el contrato'
    });
  }
}

async function listarContratos(req, res) {
  try {
    if (
      req.usuario?.rol !== 'propietario' &&
      req.usuario?.rol !== 'arrendatario'
    ) {
      return res.status(403).json({
        mensaje: 'El rol autenticado no puede consultar contratos'
      });
    }

    const contratos =
      await contratoService.listarContratosUsuario(
        req.usuario.id_usuario,
        req.usuario.rol
      );

    return res.status(200).json({
      mensaje: 'Contratos recuperados correctamente',
      total: contratos.length,
      datos: contratos
    });
  } catch (error) {
    console.error(
      'Error al listar contratos:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible consultar los contratos'
    });
  }
}

async function actualizarEstadoContrato(req, res) {
  try {
    if (req.usuario?.rol !== 'propietario') {
      return res.status(403).json({
        mensaje: 'Solo los propietarios pueden administrar contratos'
      });
    }

    const idContrato = obtenerId(req.params.id);

    if (!idContrato) {
      return res.status(400).json({
        mensaje: 'El identificador del contrato no es válido'
      });
    }

    const estado =
      typeof req.body?.estado === 'string'
        ? req.body.estado.trim().toLowerCase()
        : '';

    if (!ESTADOS_CIERRE.includes(estado)) {
      return res.status(400).json({
        mensaje: 'El estado debe ser finalizado o cancelado'
      });
    }

    const resultado =
      await contratoService.actualizarEstadoContrato(
        idContrato,
        req.usuario.id_usuario,
        estado
      );

    if (resultado.error === 'contrato_no_existe') {
      return res.status(404).json({
        mensaje: 'El contrato no existe'
      });
    }

    if (resultado.error === 'sin_permiso') {
      return res.status(403).json({
        mensaje: 'No tiene permiso para administrar este contrato'
      });
    }

    if (resultado.error === 'contrato_no_activo') {
      return res.status(409).json({
        mensaje: 'Solo se puede cambiar un contrato activo'
      });
    }

    return res.status(200).json({
      mensaje: `Contrato ${estado} correctamente`,
      datos: resultado.datos
    });
  } catch (error) {
    console.error(
      'Error al actualizar contrato:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible actualizar el contrato'
    });
  }
}

module.exports = {
  crearContrato,
  listarContratos,
  actualizarEstadoContrato
};

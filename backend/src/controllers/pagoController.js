const pagoService = require('../services/pagoService');

function obtenerId(valor) {
  const id = Number(valor);

  return Number.isInteger(id) && id > 0
    ? id
    : null;
}

function esRolParticipante(rol) {
  return (
    rol === 'propietario' ||
    rol === 'arrendatario'
  );
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

function validarPago(datos = {}) {
  const idContrato = obtenerId(
    datos.id_contrato
  );

  if (!idContrato) {
    return {
      error: 'El identificador del contrato no es válido'
    };
  }

  if (!esFechaValida(datos.fecha_pago)) {
    return {
      error: 'La fecha de pago debe tener el formato AAAA-MM-DD'
    };
  }

  const monto = Number(datos.monto);

  if (
    !Number.isFinite(monto) ||
    monto <= 0 ||
    monto > 99999999.99
  ) {
    return {
      error: 'El monto del pago no es válido'
    };
  }

  const metodoPago =
    typeof datos.metodo_pago === 'string'
      ? datos.metodo_pago.trim()
      : '';

  if (
    metodoPago.length === 0 ||
    metodoPago.length > 50
  ) {
    return {
      error: 'El método de pago es obligatorio y no puede superar 50 caracteres'
    };
  }

  const observaciones =
    typeof datos.observaciones === 'string'
      ? datos.observaciones.trim()
      : '';

  if (observaciones.length > 55) {
    return {
      error: 'Las observaciones no pueden superar 55 caracteres'
    };
  }

  return {
    datos: {
      id_contrato: idContrato,
      fecha_pago: datos.fecha_pago,
      monto,
      metodo_pago: metodoPago,
      observaciones: observaciones || null
    }
  };
}

async function crearPago(req, res) {
  try {
    if (!esRolParticipante(req.usuario?.rol)) {
      return res.status(403).json({
        mensaje: 'El rol autenticado no puede registrar pagos'
      });
    }

    const validacion = validarPago(req.body);

    if (validacion.error) {
      return res.status(400).json({
        mensaje: validacion.error
      });
    }

    const resultado = await pagoService.crearPago({
      ...validacion.datos,
      id_usuario: req.usuario.id_usuario,
      rol: req.usuario.rol
    });

    if (resultado.error === 'contrato_no_existe') {
      return res.status(404).json({
        mensaje: 'El contrato no existe'
      });
    }

    if (resultado.error === 'sin_permiso') {
      return res.status(403).json({
        mensaje: 'No tiene permiso para registrar pagos en este contrato'
      });
    }

    if (resultado.error === 'contrato_no_activo') {
      return res.status(409).json({
        mensaje: 'Solo se pueden registrar pagos en contratos activos'
      });
    }

    return res.status(201).json({
      mensaje: 'Pago registrado correctamente',
      datos: resultado.datos
    });
  } catch (error) {
    console.error(
      'Error al registrar pago:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible registrar el pago'
    });
  }
}

async function listarPagos(req, res) {
  try {
    if (!esRolParticipante(req.usuario?.rol)) {
      return res.status(403).json({
        mensaje: 'El rol autenticado no puede consultar pagos'
      });
    }

    let idContrato = null;

    if (req.query.contrato !== undefined) {
      idContrato = obtenerId(req.query.contrato);

      if (!idContrato) {
        return res.status(400).json({
          mensaje: 'El identificador del contrato no es válido'
        });
      }

      const contrato =
        await pagoService.obtenerContratoParaPago(
          idContrato
        );

      if (
        !contrato ||
        !pagoService.usuarioParticipa(
          contrato,
          req.usuario.id_usuario,
          req.usuario.rol
        )
      ) {
        return res.status(404).json({
          mensaje: 'El contrato no existe o no está disponible para este usuario'
        });
      }
    }

    const pagos =
      await pagoService.listarPagosUsuario(
        req.usuario.id_usuario,
        req.usuario.rol,
        idContrato
      );

    return res.status(200).json({
      mensaje: 'Pagos recuperados correctamente',
      total: pagos.length,
      datos: pagos
    });
  } catch (error) {
    console.error(
      'Error al listar pagos:',
      error.message
    );

    return res.status(500).json({
      mensaje: 'No fue posible consultar los pagos'
    });
  }
}

module.exports = {
  crearPago,
  listarPagos
};

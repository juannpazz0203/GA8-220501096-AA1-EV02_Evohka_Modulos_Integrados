const {
  MAX_FOTOGRAFIAS_POR_PROPIEDAD
} = require('../config/fotografias');
const {
  fotoPropiedadRepository
} = require('../repositories/fotoPropiedadRepository');
const {
  almacenamientoFotografias
} = require('./almacenamientoFotografiaService');
const { ErrorFotoPropiedad } = require('./fotoPropiedadErrores');

function idPositivo(valor, nombre) {
  const numero = Number(valor);
  if (!Number.isInteger(numero) || numero <= 0) {
    throw new ErrorFotoPropiedad(
      'IDENTIFICADOR_INVALIDO',
      `El ${nombre} no es válido`,
      400
    );
  }
  return numero;
}

function crearFotoPropiedadService(opciones = {}) {
  const repositorio = opciones.repositorio || fotoPropiedadRepository;
  const almacenamiento =
    opciones.almacenamiento || almacenamientoFotografias;

  function mapearFotografia(fotografia) {
    const idPropiedad = Number(fotografia.id_propiedad);
    const idFoto = Number(fotografia.id_foto);

    return {
      id_foto: idFoto,
      id_propiedad: idPropiedad,
      mime_type: fotografia.mime_type,
      tamano_bytes: Number(fotografia.tamano_bytes),
      orden: Number(fotografia.orden),
      es_portada: Number(fotografia.orden) === 0,
      fecha_creacion: fotografia.fecha_creacion,
      ruta_http:
        `/api/propiedades/${idPropiedad}/fotos/${idFoto}/archivo`
    };
  }

  function comprobarPropiedad(propiedad, idUsuario, soloPropietario) {
    if (!propiedad) {
      throw new ErrorFotoPropiedad(
        'PROPIEDAD_NO_EXISTE',
        'La propiedad no existe',
        404
      );
    }

    if (
      soloPropietario &&
      Number(propiedad.id_propietario) !== Number(idUsuario)
    ) {
      throw new ErrorFotoPropiedad(
        'SIN_PERMISO',
        'No tiene permiso para administrar las fotografías de esta propiedad',
        403
      );
    }
  }

  async function listarFotografias(idPropiedad) {
    const id = idPositivo(idPropiedad, 'identificador de la propiedad');
    const propiedad = await repositorio.obtenerPropiedad(id);
    comprobarPropiedad(propiedad, null, false);

    const fotografias = await repositorio.listarFotografias(id);
    return fotografias.map(mapearFotografia);
  }

  async function subirFotografias(idPropiedad, idUsuario, archivos = []) {
    const id = idPositivo(idPropiedad, 'identificador de la propiedad');
    const usuario = idPositivo(idUsuario, 'identificador del usuario');

    if (!Array.isArray(archivos) || archivos.length === 0) {
      throw new ErrorFotoPropiedad(
        'FOTOGRAFIAS_REQUERIDAS',
        'Debe seleccionar al menos una fotografía',
        400
      );
    }

    if (archivos.length > MAX_FOTOGRAFIAS_POR_PROPIEDAD) {
      throw new ErrorFotoPropiedad(
        'LIMITE_FOTOGRAFIAS',
        'Una propiedad puede tener como máximo 10 fotografías',
        409
      );
    }

    const propiedadInicial = await repositorio.obtenerPropiedad(id);
    comprobarPropiedad(propiedadInicial, usuario, true);

    const imagenesProcesadas = [];
    for (const archivo of archivos) {
      imagenesProcesadas.push(
        await almacenamiento.procesarImagen(archivo)
      );
    }

    const rutasGuardadas = [];

    try {
      const fotografiasCreadas = await repositorio.conTransaccion(
        async (transaccion) => {
          const propiedad = await transaccion.obtenerPropiedad(id, true);
          comprobarPropiedad(propiedad, usuario, true);

          const totalActual = await transaccion.contarFotografias(id);
          if (
            totalActual + imagenesProcesadas.length >
            MAX_FOTOGRAFIAS_POR_PROPIEDAD
          ) {
            throw new ErrorFotoPropiedad(
              'LIMITE_FOTOGRAFIAS',
              'Una propiedad puede tener como máximo 10 fotografías',
              409
            );
          }

          let orden = (await transaccion.obtenerMayorOrden(id)) + 1;
          const creadas = [];

          for (const imagen of imagenesProcesadas) {
            const archivoGuardado =
              await almacenamiento.guardarImagen(id, imagen);
            rutasGuardadas.push(archivoGuardado.ruta_relativa);

            const idFoto = await transaccion.insertarFotografia({
              id_propiedad: id,
              ruta_relativa: archivoGuardado.ruta_relativa,
              mime_type: imagen.mime_type,
              tamano_bytes: imagen.tamano_bytes,
              orden
            });

            creadas.push(
              mapearFotografia({
                id_foto: idFoto,
                id_propiedad: id,
                mime_type: imagen.mime_type,
                tamano_bytes: imagen.tamano_bytes,
                orden,
                fecha_creacion: new Date().toISOString()
              })
            );
            orden += 1;
          }

          return creadas;
        }
      );

      return fotografiasCreadas;
    } catch (error) {
      const limpiezas = await Promise.allSettled(
        rutasGuardadas.map((ruta) => almacenamiento.eliminarArchivo(ruta))
      );
      const fallidas = limpiezas.filter(
        (resultado) => resultado.status === 'rejected'
      ).length;
      if (fallidas > 0) {
        console.error('No fue posible limpiar archivos de una carga fallida:', {
          cantidad: fallidas
        });
      }
      throw error;
    }
  }

  async function eliminarFotografia(
    idPropiedad,
    idFoto,
    idUsuario
  ) {
    const id = idPositivo(idPropiedad, 'identificador de la propiedad');
    const foto = idPositivo(idFoto, 'identificador de la fotografía');
    const usuario = idPositivo(idUsuario, 'identificador del usuario');
    let movimiento = null;
    let transaccionConfirmada = false;

    try {
      const eliminada = await repositorio.conTransaccion(
        async (transaccion) => {
          const propiedad = await transaccion.obtenerPropiedad(id, true);
          comprobarPropiedad(propiedad, usuario, true);

          const fotografia = await transaccion.obtenerFotografia(
            id,
            foto,
            true
          );

          if (!fotografia) {
            throw new ErrorFotoPropiedad(
              'FOTOGRAFIA_NO_EXISTE',
              'La fotografía no existe',
              404
            );
          }

          movimiento = await almacenamiento.moverArchivoACuarentena(
            fotografia.ruta_relativa
          );

          await transaccion.eliminarFotografia(id, foto);
          const restantes = await transaccion.listarFotografias(id, true);

          if (restantes.length > 0) {
            await transaccion.desplazarOrdenTemporal(id);
            for (const [indice, restante] of restantes.entries()) {
              await transaccion.actualizarOrden(
                id,
                restante.id_foto,
                indice
              );
            }
          }

          return mapearFotografia(fotografia);
        }
      );

      transaccionConfirmada = true;
      try {
        await almacenamiento.confirmarCuarentena(movimiento);
      } catch (error) {
        console.error('No fue posible limpiar una fotografía eliminada:', {
          codigo: error.code || 'ERROR_LIMPIEZA_FOTOGRAFIA'
        });
      }
      return eliminada;
    } catch (error) {
      if (!transaccionConfirmada && movimiento?.existia) {
        await almacenamiento.restaurarCuarentena(movimiento);
      }
      throw error;
    }
  }

  async function reordenarFotografias(
    idPropiedad,
    idUsuario,
    ordenFotos,
    idPortada
  ) {
    const id = idPositivo(idPropiedad, 'identificador de la propiedad');
    const usuario = idPositivo(idUsuario, 'identificador del usuario');

    if (!Array.isArray(ordenFotos) || ordenFotos.length === 0) {
      throw new ErrorFotoPropiedad(
        'ORDEN_INVALIDO',
        'Debe enviar el orden completo de las fotografías',
        400
      );
    }

    const ids = ordenFotos.map((valor) =>
      idPositivo(valor, 'identificador de fotografía')
    );
    const portada = idPositivo(
      idPortada,
      'identificador de la fotografía de portada'
    );

    if (new Set(ids).size !== ids.length || !ids.includes(portada)) {
      throw new ErrorFotoPropiedad(
        'ORDEN_INVALIDO',
        'El orden contiene fotografías repetidas o una portada inválida',
        400
      );
    }

    await repositorio.conTransaccion(async (transaccion) => {
      const propiedad = await transaccion.obtenerPropiedad(id, true);
      comprobarPropiedad(propiedad, usuario, true);

      const existentes = await transaccion.listarFotografias(id, true);
      const existentesIds = existentes
        .map((item) => Number(item.id_foto))
        .sort((a, b) => a - b);
      const recibidosIds = [...ids].sort((a, b) => a - b);

      if (
        existentesIds.length !== recibidosIds.length ||
        existentesIds.some((valor, indice) => valor !== recibidosIds[indice])
      ) {
        throw new ErrorFotoPropiedad(
          'ORDEN_INCOMPLETO',
          'Debe enviar exactamente todas las fotografías de la propiedad',
          409
        );
      }

      const ordenFinal = [portada, ...ids.filter((fotoId) => fotoId !== portada)];
      await transaccion.desplazarOrdenTemporal(id);

      for (const [indice, fotoId] of ordenFinal.entries()) {
        await transaccion.actualizarOrden(id, fotoId, indice);
      }
    });

    return listarFotografias(id);
  }

  async function obtenerArchivo(idPropiedad, idFoto) {
    const id = idPositivo(idPropiedad, 'identificador de la propiedad');
    const foto = idPositivo(idFoto, 'identificador de la fotografía');
    const fotografia = await repositorio.obtenerFotografia(id, foto);

    if (!fotografia) {
      throw new ErrorFotoPropiedad(
        'FOTOGRAFIA_NO_EXISTE',
        'La fotografía no existe',
        404
      );
    }

    return {
      rutaAbsoluta: await almacenamiento.obtenerArchivo(
        fotografia.ruta_relativa
      ),
      mimeType: fotografia.mime_type
    };
  }

  async function prepararEliminacionPropiedad(idPropiedad) {
    return almacenamiento.moverDirectorioACuarentena(idPropiedad);
  }

  async function cancelarEliminacionPropiedad(movimiento) {
    await almacenamiento.restaurarDirectorioCuarentena(movimiento);
  }

  async function confirmarEliminacionPropiedad(movimiento) {
    await almacenamiento.confirmarDirectorioCuarentena(movimiento);
  }

  return {
    listarFotografias,
    subirFotografias,
    eliminarFotografia,
    reordenarFotografias,
    obtenerArchivo,
    prepararEliminacionPropiedad,
    cancelarEliminacionPropiedad,
    confirmarEliminacionPropiedad
  };
}

const fotoPropiedadService = crearFotoPropiedadService();

module.exports = {
  ...fotoPropiedadService,
  crearFotoPropiedadService
};

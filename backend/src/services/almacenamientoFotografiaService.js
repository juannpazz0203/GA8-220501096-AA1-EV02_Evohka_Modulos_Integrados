const fs = require('fs/promises');
const path = require('path');
const { randomUUID } = require('crypto');
const sharp = require('sharp');
const {
  MAX_BYTES_FOTOGRAFIA,
  MAX_LADO_FOTOGRAFIA,
  CALIDAD_WEBP,
  MAX_PIXELES_ENTRADA,
  DIRECTORIO_STORAGE
} = require('../config/fotografias');
const { ErrorFotoPropiedad } = require('./fotoPropiedadErrores');

const FORMATOS_ADMITIDOS = new Set(['jpeg', 'png', 'webp']);

function idPositivo(valor, nombre = 'identificador') {
  const numero = Number(valor);
  if (!Number.isInteger(numero) || numero <= 0) {
    throw new ErrorFotoPropiedad(
      'IDENTIFICADOR_INVALIDO',
      `El ${nombre} no es válido`
    );
  }
  return numero;
}

function crearAlmacenamientoFotografias(opciones = {}) {
  const directorioRaiz = path.resolve(
    opciones.directorioRaiz || DIRECTORIO_STORAGE
  );

  function resolverRutaSegura(rutaRelativa) {
    if (
      typeof rutaRelativa !== 'string' ||
      rutaRelativa.trim() === '' ||
      path.isAbsolute(rutaRelativa) ||
      rutaRelativa.includes('\0')
    ) {
      throw new ErrorFotoPropiedad(
        'RUTA_INSEGURA',
        'La ruta de la fotografía no es segura',
        500
      );
    }

    const segmentos = rutaRelativa.split(/[\\/]/);
    if (
      segmentos.some(
        (segmento) =>
          segmento === '' || segmento === '.' || segmento === '..'
      )
    ) {
      throw new ErrorFotoPropiedad(
        'RUTA_INSEGURA',
        'La ruta de la fotografía no es segura',
        500
      );
    }

    const rutaAbsoluta = path.resolve(directorioRaiz, ...segmentos);
    const rutaDesdeRaiz = path.relative(directorioRaiz, rutaAbsoluta);

    if (
      rutaDesdeRaiz === '' ||
      rutaDesdeRaiz.startsWith(`..${path.sep}`) ||
      rutaDesdeRaiz === '..' ||
      path.isAbsolute(rutaDesdeRaiz)
    ) {
      throw new ErrorFotoPropiedad(
        'RUTA_INSEGURA',
        'La ruta de la fotografía no es segura',
        500
      );
    }

    return rutaAbsoluta;
  }

  async function procesarImagen(archivo) {
    const buffer = archivo?.buffer;
    const rutaTemporal = archivo?.path;
    const origen = Buffer.isBuffer(buffer) ? buffer : rutaTemporal;
    let tamanoOriginal = Buffer.isBuffer(buffer)
      ? buffer.length
      : Number(archivo?.size);

    if (
      !Buffer.isBuffer(origen) &&
      (typeof origen !== 'string' || origen.trim() === '')
    ) {
      throw new ErrorFotoPropiedad(
        'ARCHIVO_INVALIDO',
        'La fotografía está vacía o dañada',
        400
      );
    }

    if (!Number.isFinite(tamanoOriginal)) {
      const estado = await fs.stat(origen);
      tamanoOriginal = estado.size;
    }

    if (tamanoOriginal <= 0) {
      throw new ErrorFotoPropiedad(
        'ARCHIVO_INVALIDO',
        'La fotografía está vacía o dañada',
        400
      );
    }

    if (tamanoOriginal > MAX_BYTES_FOTOGRAFIA) {
      throw new ErrorFotoPropiedad(
        'ARCHIVO_DEMASIADO_GRANDE',
        'Cada fotografía debe pesar como máximo 15 MB',
        413
      );
    }

    try {
      const imagen = sharp(origen, {
        failOn: 'error',
        limitInputPixels: MAX_PIXELES_ENTRADA,
        sequentialRead: true
      });
      const metadata = await imagen.metadata();

      if (
        !FORMATOS_ADMITIDOS.has(metadata.format) ||
        !metadata.width ||
        !metadata.height ||
        (metadata.pages || 1) !== 1
      ) {
        throw new ErrorFotoPropiedad(
          'FORMATO_NO_ADMITIDO',
          'Solo se admiten fotografías JPG, PNG o WebP no animadas',
          415
        );
      }

      const resultado = await imagen
        .rotate()
        .resize({
          width: MAX_LADO_FOTOGRAFIA,
          height: MAX_LADO_FOTOGRAFIA,
          fit: 'inside',
          withoutEnlargement: true
        })
        .webp({
          quality: CALIDAD_WEBP,
          effort: 4
        })
        .toBuffer({ resolveWithObject: true });

      return {
        buffer: resultado.data,
        mime_type: 'image/webp',
        tamano_bytes: resultado.data.length,
        ancho: resultado.info.width,
        alto: resultado.info.height
      };
    } catch (error) {
      if (error instanceof ErrorFotoPropiedad) throw error;

      throw new ErrorFotoPropiedad(
        'ARCHIVO_INVALIDO',
        'La fotografía está dañada o su contenido no es válido',
        400,
        error
      );
    }
  }

  async function guardarImagen(idPropiedad, imagenProcesada) {
    const id = idPositivo(idPropiedad, 'identificador de la propiedad');
    const nombreArchivo = `${randomUUID()}.webp`;
    const rutaRelativa = `propiedades/${id}/${nombreArchivo}`;
    const rutaAbsoluta = resolverRutaSegura(rutaRelativa);

    await fs.mkdir(path.dirname(rutaAbsoluta), { recursive: true });
    await fs.writeFile(rutaAbsoluta, imagenProcesada.buffer, { flag: 'wx' });

    return {
      ruta_relativa: rutaRelativa,
      ruta_absoluta: rutaAbsoluta
    };
  }

  async function eliminarArchivo(rutaRelativa) {
    const rutaAbsoluta = resolverRutaSegura(rutaRelativa);
    try {
      await fs.unlink(rutaAbsoluta);
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }
  }

  async function moverArchivoACuarentena(rutaRelativa) {
    const rutaOriginal = resolverRutaSegura(rutaRelativa);
    const rutaCuarentenaRelativa = `.papelera/${randomUUID()}.webp`;
    const rutaCuarentena = resolverRutaSegura(rutaCuarentenaRelativa);

    await fs.mkdir(path.dirname(rutaCuarentena), { recursive: true });

    try {
      await fs.rename(rutaOriginal, rutaCuarentena);
      return {
        existia: true,
        rutaOriginal,
        rutaCuarentena
      };
    } catch (error) {
      if (error.code === 'ENOENT') {
        return { existia: false };
      }
      throw error;
    }
  }

  async function restaurarCuarentena(movimiento) {
    if (!movimiento?.existia) return;
    await fs.mkdir(path.dirname(movimiento.rutaOriginal), { recursive: true });
    await fs.rename(movimiento.rutaCuarentena, movimiento.rutaOriginal);
  }

  async function confirmarCuarentena(movimiento) {
    if (!movimiento?.existia) return;
    try {
      await fs.unlink(movimiento.rutaCuarentena);
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }
  }

  async function obtenerArchivo(rutaRelativa) {
    const rutaAbsoluta = resolverRutaSegura(rutaRelativa);
    try {
      const estado = await fs.stat(rutaAbsoluta);
      if (!estado.isFile()) throw new Error('La ruta no corresponde a un archivo');
      return rutaAbsoluta;
    } catch (error) {
      if (error.code === 'ENOENT') {
        throw new ErrorFotoPropiedad(
          'ARCHIVO_NO_ENCONTRADO',
          'El archivo de la fotografía no está disponible',
          404
        );
      }
      throw error;
    }
  }

  async function moverDirectorioACuarentena(idPropiedad) {
    const id = idPositivo(idPropiedad, 'identificador de la propiedad');
    const rutaOriginal = resolverRutaSegura(`propiedades/${id}`);
    const rutaCuarentena = resolverRutaSegura(
      `.papelera/propiedad-${id}-${randomUUID()}`
    );
    await fs.mkdir(path.dirname(rutaCuarentena), { recursive: true });

    try {
      await fs.rename(rutaOriginal, rutaCuarentena);
      return {
        existia: true,
        rutaOriginal,
        rutaCuarentena
      };
    } catch (error) {
      if (error.code === 'ENOENT') return { existia: false };
      throw error;
    }
  }

  async function restaurarDirectorioCuarentena(movimiento) {
    if (!movimiento?.existia) return;
    await fs.mkdir(path.dirname(movimiento.rutaOriginal), { recursive: true });
    await fs.rename(movimiento.rutaCuarentena, movimiento.rutaOriginal);
  }

  async function confirmarDirectorioCuarentena(movimiento) {
    if (!movimiento?.existia) return;
    await fs.rm(movimiento.rutaCuarentena, {
      recursive: true,
      force: true
    });
  }

  return {
    directorioRaiz,
    resolverRutaSegura,
    procesarImagen,
    guardarImagen,
    eliminarArchivo,
    moverArchivoACuarentena,
    restaurarCuarentena,
    confirmarCuarentena,
    obtenerArchivo,
    moverDirectorioACuarentena,
    restaurarDirectorioCuarentena,
    confirmarDirectorioCuarentena
  };
}

const almacenamientoFotografias = crearAlmacenamientoFotografias();

module.exports = {
  crearAlmacenamientoFotografias,
  almacenamientoFotografias
};

const multer = require('multer');
const fs = require('fs');
const fsPromises = require('fs/promises');
const path = require('path');
const { randomUUID } = require('crypto');
const {
  MAX_FOTOGRAFIAS_POR_PROPIEDAD,
  MAX_BYTES_FOTOGRAFIA,
  MIME_ADMITIDOS,
  DIRECTORIO_TEMPORAL
} = require('../config/fotografias');
const { ErrorFotoPropiedad } = require('../services/fotoPropiedadErrores');

const almacenamientoTemporal = multer.diskStorage({
  destination: (req, archivo, callback) => {
    fs.mkdir(DIRECTORIO_TEMPORAL, { recursive: true }, (error) => {
      callback(error, DIRECTORIO_TEMPORAL);
    });
  },
  filename: (req, archivo, callback) => {
    callback(null, `${randomUUID()}.upload`);
  }
});

async function limpiarTemporales(archivos = []) {
  const resultados = await Promise.allSettled(
    archivos.map(async (archivo) => {
      if (typeof archivo?.path !== 'string') return;
      const ruta = path.resolve(archivo.path);
      const relativa = path.relative(DIRECTORIO_TEMPORAL, ruta);
      if (
        relativa === '' ||
        relativa === '..' ||
        relativa.startsWith(`..${path.sep}`) ||
        path.isAbsolute(relativa)
      ) {
        return;
      }
      try {
        await fsPromises.unlink(ruta);
      } catch (error) {
        if (error.code !== 'ENOENT') throw error;
      }
    })
  );

  const fallidas = resultados.filter(
    (resultado) => resultado.status === 'rejected'
  ).length;
  if (fallidas > 0) {
    console.error('No fue posible limpiar archivos temporales:', {
      cantidad: fallidas
    });
  }
}

const carga = multer({
  storage: almacenamientoTemporal,
  limits: {
    fileSize: MAX_BYTES_FOTOGRAFIA,
    files: MAX_FOTOGRAFIAS_POR_PROPIEDAD,
    fields: 2,
    parts: MAX_FOTOGRAFIAS_POR_PROPIEDAD + 2
  },
  fileFilter: (req, archivo, callback) => {
    if (!MIME_ADMITIDOS.has(archivo.mimetype)) {
      return callback(
        new ErrorFotoPropiedad(
          'FORMATO_NO_ADMITIDO',
          'Solo se admiten fotografías JPG, PNG o WebP',
          415
        )
      );
    }

    return callback(null, true);
  }
});

function recibirFotografias(req, res, next) {
  carga.array('fotos', MAX_FOTOGRAFIAS_POR_PROPIEDAD)(
    req,
    res,
    (error) => {
      if (!error) return next();

      void limpiarTemporales(req.files);

      if (error instanceof ErrorFotoPropiedad) {
        return res.status(error.estadoHttp).json({
          mensaje: error.message,
          codigo: error.codigo
        });
      }

      if (error instanceof multer.MulterError) {
        const archivoGrande = error.code === 'LIMIT_FILE_SIZE';
        return res.status(archivoGrande ? 413 : 400).json({
          mensaje: archivoGrande
            ? 'Cada fotografía debe pesar como máximo 15 MB'
            : 'La carga contiene demasiados archivos o campos',
          codigo: error.code
        });
      }

      console.error('Error al recibir fotografías:', {
        codigo: error.code || 'ERROR_MULTIPART'
      });
      return res.status(400).json({
        mensaje: 'No fue posible procesar la carga de fotografías'
      });
    }
  );

  let limpiezaIniciada = false;
  const limpiarAlFinalizar = () => {
    if (limpiezaIniciada) return;
    limpiezaIniciada = true;
    void limpiarTemporales(req.files);
  };
  res.once('finish', limpiarAlFinalizar);
  res.once('close', limpiarAlFinalizar);
}

module.exports = {
  recibirFotografias
};

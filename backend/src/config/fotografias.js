const path = require('path');

const MAX_FOTOGRAFIAS_POR_PROPIEDAD = 10;
const MAX_BYTES_FOTOGRAFIA = 15 * 1024 * 1024;
const MAX_LADO_FOTOGRAFIA = 1920;
const CALIDAD_WEBP = 85;
const MAX_PIXELES_ENTRADA = 40_000_000;
const MIME_ADMITIDOS = new Set([
  'image/jpeg',
  'image/png',
  'image/webp'
]);

const DIRECTORIO_STORAGE = path.resolve(
  __dirname,
  '..',
  '..',
  'storage'
);
const DIRECTORIO_TEMPORAL = path.join(
  DIRECTORIO_STORAGE,
  '.temporales'
);

module.exports = {
  MAX_FOTOGRAFIAS_POR_PROPIEDAD,
  MAX_BYTES_FOTOGRAFIA,
  MAX_LADO_FOTOGRAFIA,
  CALIDAD_WEBP,
  MAX_PIXELES_ENTRADA,
  MIME_ADMITIDOS,
  DIRECTORIO_STORAGE,
  DIRECTORIO_TEMPORAL
};

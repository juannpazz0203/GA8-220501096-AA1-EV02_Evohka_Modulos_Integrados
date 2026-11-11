require('dotenv').config();

const { pool } = require('../src/config/db');
const contrasenaService = require('../src/services/contrasenaService');

const VARIABLE_MODO = 'EVOHKA_MIGRACION_MODO';
const VARIABLE_BASE_ESPERADA = 'EVOHKA_MIGRACION_BASE_ESPERADA';
const VARIABLE_CONFIRMACION = 'EVOHKA_CONFIRMAR_MIGRACION_CONTRASENAS';
const VARIABLE_AUTORIZACION_PRODUCCION =
  'EVOHKA_AUTORIZAR_BASE_PRODUCCION';
const VARIABLE_RESPALDO_PRODUCCION =
  'EVOHKA_CONFIRMAR_RESPALDO_PRODUCCION';
const TAMANO_LOTE_PREDETERMINADO = 50;
const CONFIGURACIONES = Object.freeze({
  validacion: Object.freeze({
    base: 'gestion_arriendos_respaldo_test',
    confirmacion: 'MIGRAR_BCRYPT_RNF03'
  }),
  produccion: Object.freeze({
    base: 'gestion_arriendos',
    confirmacion: 'MIGRAR_BCRYPT_RNF03_PRODUCCION',
    autorizacionBase: 'AUTORIZO_GESTION_ARRIENDOS_RNF03',
    confirmacionRespaldo: 'RESPALDO_RNF03_PRODUCCION_VERIFICADO'
  })
});
let etapaActual = 'validacion_configuracion';

function crearError(codigo, mensaje) {
  return Object.assign(new Error(mensaje), { code: codigo });
}

function valorEntorno(nombre) {
  return String(process.env[nombre] || '').trim();
}

function resolverConfiguracion() {
  const modo = valorEntorno(VARIABLE_MODO).toLowerCase();
  const configuracion = CONFIGURACIONES[modo];
  if (!configuracion) {
    throw crearError(
      'MODO_MIGRACION_REQUERIDO',
      'El modo debe ser validacion o produccion'
    );
  }

  const baseEsperada = valorEntorno(VARIABLE_BASE_ESPERADA);
  if (!baseEsperada) {
    throw crearError(
      'BASE_ESPERADA_REQUERIDA',
      'Falta la base esperada para la migración'
    );
  }
  if (baseEsperada !== configuracion.base) {
    throw crearError(
      'BASE_ESPERADA_NO_COINCIDE_CON_MODO',
      'La base esperada no corresponde al modo solicitado'
    );
  }

  const baseConfigurada = valorEntorno('DB_NAME');
  if (!baseConfigurada) {
    throw crearError(
      'DB_NAME_EXPLICITA_REQUERIDA',
      'DB_NAME debe definirse expresamente para ejecutar la migración'
    );
  }
  if (baseConfigurada !== baseEsperada) {
    throw crearError(
      'DB_NAME_NO_COINCIDE',
      'DB_NAME no coincide con la base esperada'
    );
  }

  if (valorEntorno(VARIABLE_CONFIRMACION) !== configuracion.confirmacion) {
    throw crearError(
      'CONFIRMACION_REQUERIDA',
      'Falta la confirmación explícita del modo solicitado'
    );
  }

  if (modo === 'produccion') {
    if (
      valorEntorno(VARIABLE_AUTORIZACION_PRODUCCION) !==
      configuracion.autorizacionBase
    ) {
      throw crearError(
        'AUTORIZACION_BASE_PRODUCCION_REQUERIDA',
        'Falta la autorización explícita de la base de producción'
      );
    }
    if (
      valorEntorno(VARIABLE_RESPALDO_PRODUCCION) !==
      configuracion.confirmacionRespaldo
    ) {
      throw crearError(
        'CONFIRMACION_RESPALDO_PRODUCCION_REQUERIDA',
        'Falta confirmar el respaldo restaurable de producción'
      );
    }
  }

  return { modo, ...configuracion };
}

function tamanoLote() {
  const configurado = Number(process.env.EVOHKA_MIGRACION_LOTE);
  return Number.isInteger(configurado) && configurado >= 1 && configurado <= 200
    ? configurado
    : TAMANO_LOTE_PREDETERMINADO;
}

async function prepararCredencial(credencial) {
  const hashExistente = contrasenaService.extraerHashEnvuelto(credencial);
  if (hashExistente) return hashExistente;
  return contrasenaService.crearHashParaMigracion(credencial);
}

async function convertirLote(filas, conexion) {
  etapaActual = 'preparacion_lote';
  const preparadas = [];
  let omitidas = 0;

  for (const fila of filas) {
    const hash = await prepararCredencial(fila.contrasena);
    if (!hash) {
      omitidas += 1;
      continue;
    }
    preparadas.push({
      idUsuario: fila.id_usuario,
      credencialAnterior: fila.contrasena,
      hash
    });
  }

  let convertidas = 0;
  let concurrentes = 0;
  let transaccionIniciada = false;

  try {
    etapaActual = 'inicio_transaccion';
    await conexion.beginTransaction();
    transaccionIniciada = true;
    etapaActual = 'actualizacion_lote';
    for (const preparada of preparadas) {
      const [resultado] = await conexion.execute(
        `UPDATE usuarios
         SET contrasena = ?, contrasena_algoritmo = ?
         WHERE id_usuario = ?
           AND contrasena = ?
           AND contrasena_algoritmo = ?`,
        [
          preparada.hash,
          contrasenaService.ALGORITMO_BCRYPT,
          preparada.idUsuario,
          preparada.credencialAnterior,
          contrasenaService.ALGORITMO_LEGACY
        ]
      );
      if (resultado.affectedRows === 1) convertidas += 1;
      else concurrentes += 1;
    }
    etapaActual = 'confirmacion_lote';
    await conexion.commit();
    transaccionIniciada = false;
  } catch (error) {
    const etapaFallo = etapaActual;
    if (transaccionIniciada) {
      etapaActual = 'reversion_lote';
      try {
        await conexion.rollback();
      } catch (errorReversion) {
        error.codigoReversion = errorReversion.code || 'ERROR_REVERSION';
      }
    }
    error.etapaMigracion = etapaFallo;
    throw error;
  }

  return { convertidas, omitidas, concurrentes };
}

async function verificarPrecondicionesProduccion(conexion) {
  etapaActual = 'precondiciones_produccion';
  const [filas] = await conexion.execute(
    `SELECT
       COALESCE(SUM(
         contrasena_algoritmo IS NULL
         OR CONVERT(contrasena_algoritmo USING utf8mb4)
              COLLATE utf8mb4_bin NOT IN (
                _utf8mb4'legacy_plain' COLLATE utf8mb4_bin,
                _utf8mb4'bcrypt' COLLATE utf8mb4_bin
              )
       ), 0) AS algoritmos_desconocidos,
       COALESCE(SUM(
         CONVERT(
           LEFT(contrasena, CHAR_LENGTH('{EVOHKA_BCRYPT_V1}'))
           USING utf8mb4
         ) COLLATE utf8mb4_bin =
           _utf8mb4'{EVOHKA_BCRYPT_V1}' COLLATE utf8mb4_bin
       ), 0) AS credenciales_con_marcador,
       COALESCE(SUM(
         CONVERT(contrasena_algoritmo USING utf8mb4)
           COLLATE utf8mb4_bin = _utf8mb4'bcrypt' COLLATE utf8mb4_bin
         AND NOT REGEXP_LIKE(
           CONVERT(contrasena USING utf8mb4) COLLATE utf8mb4_bin,
           _utf8mb4'^[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}$'
             COLLATE utf8mb4_bin,
           'c'
         )
       ), 0) AS bcrypt_invalidas,
       COALESCE(SUM(
         CONVERT(contrasena_algoritmo USING utf8mb4)
           COLLATE utf8mb4_bin =
             _utf8mb4'legacy_plain' COLLATE utf8mb4_bin
         AND OCTET_LENGTH(contrasena) > 72
       ), 0) AS legacy_mayores_72_bytes
     FROM usuarios`
  );
  const resumen = filas[0] || {};

  if (Number(resumen.algoritmos_desconocidos) !== 0) {
    throw crearError(
      'ALGORITMOS_DESCONOCIDOS',
      'Existen clasificaciones de credencial no reconocidas'
    );
  }
  if (Number(resumen.credenciales_con_marcador) !== 0) {
    throw crearError(
      'MARCADORES_PENDIENTES',
      'La clasificación SQL de marcadores no está completa'
    );
  }
  if (Number(resumen.bcrypt_invalidas) !== 0) {
    throw crearError(
      'BCRYPT_INVALIDAS',
      'Existen credenciales bcrypt con formato no reconocido'
    );
  }
  if (Number(resumen.legacy_mayores_72_bytes) !== 0) {
    throw crearError(
      'LEGACY_SUPERA_72_BYTES',
      'Existen credenciales legacy que no pueden migrarse sin truncamiento'
    );
  }
}

async function migrar() {
  const configuracion = resolverConfiguracion();

  const acumulado = {
    revisadas: 0,
    convertidas: 0,
    omitidas: 0,
    concurrentes: 0
  };
  let ultimoId = 0;
  const limiteLote = tamanoLote();
  etapaActual = 'conexion_base';
  const conexion = await pool.getConnection();

  try {
    etapaActual = 'validacion_base_real';
    const [bases] = await conexion.execute(
      'SELECT DATABASE() AS base_actual'
    );
    const baseActual = bases[0]?.base_actual;
    if (baseActual !== configuracion.base) {
      throw crearError(
        'BASE_REAL_NO_COINCIDE',
        'La base real no coincide con el modo autorizado'
      );
    }

    if (configuracion.modo === 'produccion') {
      await verificarPrecondicionesProduccion(conexion);
    }

    while (true) {
      etapaActual = 'lectura_lote';
      const [filas] = await conexion.execute(
        `SELECT id_usuario, contrasena
         FROM usuarios
         WHERE contrasena_algoritmo = ?
           AND id_usuario > ?
         ORDER BY id_usuario ASC
         LIMIT ${limiteLote}`,
        [
          contrasenaService.ALGORITMO_LEGACY,
          ultimoId
        ]
      );
      if (filas.length === 0) break;

      ultimoId = filas[filas.length - 1].id_usuario;
      const resultado = await convertirLote(filas, conexion);
      acumulado.revisadas += filas.length;
      acumulado.convertidas += resultado.convertidas;
      acumulado.omitidas += resultado.omitidas;
      acumulado.concurrentes += resultado.concurrentes;
    }
  } finally {
    conexion.release();
  }

  console.log('Migración de credenciales finalizada.', acumulado);
}

migrar()
  .catch((error) => {
    const diagnostico = {
      etapa: error.etapaMigracion || etapaActual,
      codigo: error.code || 'ERROR_CONTROLADO'
    };
    if (error.codigoReversion) {
      diagnostico.codigo_reversion = error.codigoReversion;
    }
    console.error('Migración de credenciales detenida.', diagnostico);
    process.exitCode = 1;
  })
  .finally(async () => {
    await pool.end();
  });

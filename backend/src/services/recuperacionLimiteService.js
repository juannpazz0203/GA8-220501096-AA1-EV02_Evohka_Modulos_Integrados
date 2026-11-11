const crypto = require('node:crypto');
const {
  VENTANA_LIMITES_MS
} = require('../config/recuperacion');

const LIMITES = Object.freeze({
  solicitudCuenta: 3,
  solicitudIp: 10,
  solicitudGlobal: 100,
  restablecimientoCuenta: 10,
  restablecimientoIp: 30,
  restablecimientoGlobal: 300
});
const MAXIMO_CLAVES = 10000;

function huellaCuenta(correo) {
  return crypto
    .createHash('sha256')
    .update(String(correo || '').trim().toLowerCase(), 'utf8')
    .digest('hex');
}

function crearAlmacenVentanas({
  ventanaMs = VENTANA_LIMITES_MS,
  ahora = () => Date.now()
} = {}) {
  const eventos = new Map();

  function eventosVigentes(clave, instante) {
    const limiteInferior = instante - ventanaMs;
    const vigentes = (eventos.get(clave) || [])
      .filter((marca) => marca > limiteInferior);

    if (vigentes.length > 0) eventos.set(clave, vigentes);
    else eventos.delete(clave);

    return vigentes;
  }

  function limitarTamano() {
    while (eventos.size > MAXIMO_CLAVES) {
      eventos.delete(eventos.keys().next().value);
    }
  }

  function consumir(reglas) {
    const instante = ahora();
    const evaluadas = reglas.map(({ clave, maximo }) => ({
      clave,
      maximo,
      vigentes: eventosVigentes(clave, instante)
    }));

    if (evaluadas.some(({ vigentes, maximo }) => vigentes.length >= maximo)) {
      return false;
    }

    for (const regla of evaluadas) {
      regla.vigentes.push(instante);
      eventos.set(regla.clave, regla.vigentes);
    }
    limitarTamano();
    return true;
  }

  return { consumir };
}

function crearRecuperacionLimiteService(opciones = {}) {
  const almacen = crearAlmacenVentanas(opciones);

  function consumirSolicitud({ correo, ip }) {
    return almacen.consumir([
      {
        clave: `solicitud:cuenta:${huellaCuenta(correo)}`,
        maximo: LIMITES.solicitudCuenta
      },
      {
        clave: `solicitud:ip:${ip || 'desconocida'}`,
        maximo: LIMITES.solicitudIp
      },
      {
        clave: 'solicitud:global',
        maximo: LIMITES.solicitudGlobal
      }
    ]);
  }

  function consumirRestablecimiento({ correo, ip }) {
    return almacen.consumir([
      {
        clave: `restablecimiento:cuenta:${huellaCuenta(correo)}`,
        maximo: LIMITES.restablecimientoCuenta
      },
      {
        clave: `restablecimiento:ip:${ip || 'desconocida'}`,
        maximo: LIMITES.restablecimientoIp
      },
      {
        clave: 'restablecimiento:global',
        maximo: LIMITES.restablecimientoGlobal
      }
    ]);
  }

  return {
    consumirSolicitud,
    consumirRestablecimiento
  };
}

module.exports = {
  LIMITES,
  crearAlmacenVentanas,
  crearRecuperacionLimiteService,
  recuperacionLimiteService: crearRecuperacionLimiteService()
};

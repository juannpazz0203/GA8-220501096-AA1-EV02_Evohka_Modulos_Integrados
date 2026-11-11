const test = require('node:test');
const assert = require('node:assert/strict');
const nodemailer = require('nodemailer');
const bcrypt = require('bcryptjs');
const {
  crearCorreoRecuperacionService
} = require('../src/services/correoRecuperacionService');
const {
  ESTADO_INVALIDO,
  ESTADO_EXITOSO,
  crearRecuperacionService
} = require('../src/services/recuperacionService');
const {
  LIMITES,
  crearRecuperacionLimiteService
} = require('../src/services/recuperacionLimiteService');
const {
  MENSAJE_NEUTRAL,
  crearRecuperacionController
} = require('../src/controllers/recuperacionController');
const authRoutes = require('../src/routes/authRoutes');

const PEPPER_SINTETICO = 'pepper-sintetico-rnf03-de-pruebas-0001';

function configuracionSintetica() {
  return {
    pepper: PEPPER_SINTETICO,
    smtp: {
      host: 'smtp.invalid',
      port: 465,
      secure: true,
      usuario: 'smtp-prueba@evohka.invalid',
      passwordAplicacion: 'valor-sintetico-no-real',
      remitente: 'EVOHKA <smtp-prueba@evohka.invalid>'
    }
  };
}

function clonarRecuperacion(recuperacion) {
  return {
    ...recuperacion,
    codigo_hash: Buffer.from(recuperacion.codigo_hash),
    codigo_salt: Buffer.from(recuperacion.codigo_salt),
    fecha_creacion: new Date(recuperacion.fecha_creacion),
    fecha_expiracion: new Date(recuperacion.fecha_expiracion),
    fecha_envio: recuperacion.fecha_envio && new Date(recuperacion.fecha_envio),
    fecha_uso: recuperacion.fecha_uso && new Date(recuperacion.fecha_uso),
    fecha_revocacion: recuperacion.fecha_revocacion &&
      new Date(recuperacion.fecha_revocacion)
  };
}

function crearRepositorioFalso() {
  const estado = {
    usuarios: [{
      id_usuario: 901,
      correo: 'cuenta.rnf03@evohka.invalid',
      contrasena: 'hash-anterior-sintetico',
      contrasena_algoritmo: 'bcrypt',
      sesion_version: 4
    }],
    recuperaciones: [],
    siguienteId: 1
  };

  const repository = {
    async conTransaccion(operacion) {
      const respaldo = {
        usuarios: estado.usuarios.map((usuario) => ({ ...usuario })),
        recuperaciones: estado.recuperaciones.map(clonarRecuperacion),
        siguienteId: estado.siguienteId
      };
      try {
        return await operacion({});
      } catch (error) {
        estado.usuarios = respaldo.usuarios;
        estado.recuperaciones = respaldo.recuperaciones;
        estado.siguienteId = respaldo.siguienteId;
        throw error;
      }
    },

    async buscarUsuarioPorCorreo(correo) {
      return estado.usuarios.find((usuario) => usuario.correo === correo) || null;
    },

    async listarSolicitudesRecientes(idUsuario, desde) {
      return estado.recuperaciones.filter((recuperacion) =>
        recuperacion.id_usuario === idUsuario &&
        recuperacion.fecha_creacion >= desde
      );
    },

    async revocarCodigosActivos(idUsuario, fecha, ejecutor, idExcluido = null) {
      let actualizadas = 0;
      for (const recuperacion of estado.recuperaciones) {
        if (
          recuperacion.id_usuario === idUsuario &&
          recuperacion.id_recuperacion !== idExcluido &&
          recuperacion.fecha_uso === null &&
          recuperacion.fecha_revocacion === null
        ) {
          recuperacion.fecha_revocacion = new Date(fecha);
          actualizadas += 1;
        }
      }
      return actualizadas;
    },

    async crearCodigo(datos) {
      const id = estado.siguienteId++;
      estado.recuperaciones.push({
        id_recuperacion: id,
        id_usuario: datos.idUsuario,
        codigo_hash: Buffer.from(datos.codigoHash),
        codigo_salt: Buffer.from(datos.codigoSalt),
        fecha_creacion: new Date(datos.fechaCreacion),
        fecha_expiracion: new Date(datos.fechaExpiracion),
        fecha_envio: null,
        intentos_fallidos: 0,
        fecha_uso: null,
        fecha_revocacion: null
      });
      return id;
    },

    async marcarCodigoEnviado(idRecuperacion, fecha) {
      const recuperacion = estado.recuperaciones.find(
        (actual) => actual.id_recuperacion === idRecuperacion
      );
      if (
        !recuperacion ||
        recuperacion.fecha_envio ||
        recuperacion.fecha_uso ||
        recuperacion.fecha_revocacion
      ) return false;
      recuperacion.fecha_envio = new Date(fecha);
      return true;
    },

    async revocarCodigo(idRecuperacion, fecha) {
      const recuperacion = estado.recuperaciones.find(
        (actual) => actual.id_recuperacion === idRecuperacion
      );
      if (!recuperacion || recuperacion.fecha_uso || recuperacion.fecha_revocacion) {
        return false;
      }
      recuperacion.fecha_revocacion = new Date(fecha);
      return true;
    },

    async obtenerUltimoCodigo(idUsuario) {
      return estado.recuperaciones
        .filter((recuperacion) => recuperacion.id_usuario === idUsuario)
        .sort((a, b) => b.id_recuperacion - a.id_recuperacion)[0] || null;
    },

    async registrarIntentoFallido(idRecuperacion, intentos, fechaRevocacion) {
      const recuperacion = estado.recuperaciones.find(
        (actual) => actual.id_recuperacion === idRecuperacion
      );
      if (!recuperacion || recuperacion.fecha_uso || recuperacion.fecha_revocacion) {
        return false;
      }
      recuperacion.intentos_fallidos = intentos;
      if (fechaRevocacion) {
        recuperacion.fecha_revocacion = new Date(fechaRevocacion);
      }
      return true;
    },

    async actualizarContrasenaYVersion(idUsuario, hash) {
      const usuario = estado.usuarios.find(
        (actual) => actual.id_usuario === idUsuario
      );
      if (!usuario) return false;
      usuario.contrasena = hash;
      usuario.contrasena_algoritmo = 'bcrypt';
      usuario.sesion_version += 1;
      return true;
    },

    async marcarCodigoUsado(idRecuperacion, fecha) {
      const recuperacion = estado.recuperaciones.find(
        (actual) => actual.id_recuperacion === idRecuperacion
      );
      if (
        !recuperacion ||
        !recuperacion.fecha_envio ||
        recuperacion.fecha_uso ||
        recuperacion.fecha_revocacion
      ) return false;
      recuperacion.fecha_uso = new Date(fecha);
      return true;
    },

    estado
  };

  return repository;
}

function crearEscenario({ servicioContrasena, servicioCorreo } = {}) {
  const repository = crearRepositorioFalso();
  let instante = new Date('2026-11-10T12:00:00.000Z');
  const mensajes = [];
  const transporteJson = nodemailer.createTransport({ jsonTransport: true });
  const correoJson = crearCorreoRecuperacionService({
    obtenerConfiguracion: configuracionSintetica,
    crearTransporte: () => ({
      async sendMail(opciones) {
        const resultado = await transporteJson.sendMail(opciones);
        mensajes.push(opciones);
        return resultado;
      }
    })
  });
  const servicio = crearRecuperacionService({
    repository,
    servicioCorreo: servicioCorreo || correoJson,
    servicioContrasena,
    obtenerConfiguracion: configuracionSintetica,
    ahora: () => new Date(instante)
  });

  return {
    repository,
    servicio,
    mensajes,
    avanzar(ms) {
      instante = new Date(instante.getTime() + ms);
    }
  };
}

function servicioContrasenaRapido() {
  return {
    async crearHash() {
      return '$2b$12$hash.sintetico.para.pruebas.sin.valor.real';
    }
  };
}

function respuesta() {
  return {
    statusCode: 200,
    body: null,
    status(codigo) {
      this.statusCode = codigo;
      return this;
    },
    json(cuerpo) {
      this.body = cuerpo;
      return this;
    }
  };
}

test('recuperación de contraseña RNF03', { concurrency: false }, async (t) => {
  await t.test('crea un código de ocho dígitos y almacena solo HMAC y salt', async () => {
    const escenario = crearEscenario();
    const resultado = await escenario.servicio.solicitarRecuperacion(
      'CUENTA.RNF03@EVOHKA.INVALID'
    );
    assert.equal(resultado.estado, 'enviado');
    assert.equal(escenario.mensajes.length, 1);
    assert.match(escenario.mensajes[0].text, /\b\d{8}\b/);

    const registro = escenario.repository.estado.recuperaciones[0];
    assert.equal(registro.codigo_hash.length, 32);
    assert.equal(registro.codigo_salt.length, 16);
    assert.equal(Object.hasOwn(registro, 'codigo'), false);
    assert.ok(registro.fecha_envio instanceof Date);
  });

  await t.test('una cuenta inexistente no envía correo ni revela su ausencia', async () => {
    const escenario = crearEscenario();
    const resultado = await escenario.servicio.solicitarRecuperacion(
      'inexistente@evohka.invalid'
    );
    assert.equal(resultado.estado, 'sin_cuenta');
    assert.equal(escenario.mensajes.length, 0);
    assert.equal(escenario.repository.estado.recuperaciones.length, 0);
  });

  await t.test('un error SMTP revoca el código que no fue enviado', async () => {
    const escenario = crearEscenario({
      servicioCorreo: {
        async enviarCodigo() {
          const error = new Error('fallo sintético');
          error.code = 'SMTP_SINTETICO';
          throw error;
        }
      }
    });
    await assert.rejects(
      escenario.servicio.solicitarRecuperacion(
        'cuenta.rnf03@evohka.invalid'
      ),
      (error) => error.code === 'ENVIO_RECUPERACION_FALLIDO'
    );
    assert.ok(
      escenario.repository.estado.recuperaciones[0].fecha_revocacion instanceof Date
    );
    assert.equal(
      escenario.repository.estado.recuperaciones[0].fecha_envio,
      null
    );
  });

  await t.test('limita persistentemente a tres solicitudes por cuenta y ventana', async () => {
    const escenario = crearEscenario();
    for (let indice = 0; indice < 3; indice += 1) {
      const resultado = await escenario.servicio.solicitarRecuperacion(
        'cuenta.rnf03@evohka.invalid'
      );
      assert.equal(resultado.estado, 'enviado');
    }
    const cuarta = await escenario.servicio.solicitarRecuperacion(
      'cuenta.rnf03@evohka.invalid'
    );
    assert.equal(cuarta.estado, 'limitado');
    assert.equal(escenario.mensajes.length, 3);
  });

  await t.test('rechaza código incorrecto y lo revoca al quinto intento', async () => {
    const escenario = crearEscenario({
      servicioContrasena: servicioContrasenaRapido()
    });
    await escenario.servicio.solicitarRecuperacion(
      'cuenta.rnf03@evohka.invalid'
    );
    for (let intento = 1; intento <= 5; intento += 1) {
      const resultado = await escenario.servicio.restablecerContrasena({
        correo: 'cuenta.rnf03@evohka.invalid',
        codigo: '00000000',
        nuevaContrasena: 'valor-sintetico-nuevo'
      });
      assert.equal(resultado.estado, ESTADO_INVALIDO);
    }
    const registro = escenario.repository.estado.recuperaciones[0];
    assert.equal(registro.intentos_fallidos, 5);
    assert.ok(registro.fecha_revocacion instanceof Date);
  });

  await t.test('rechaza códigos vencidos, revocados y no enviados', async () => {
    const escenario = crearEscenario({
      servicioContrasena: servicioContrasenaRapido()
    });
    await escenario.servicio.solicitarRecuperacion(
      'cuenta.rnf03@evohka.invalid'
    );
    const registro = escenario.repository.estado.recuperaciones[0];
    const codigo = escenario.mensajes[0].text.match(/\b\d{8}\b/)[0];

    escenario.avanzar(15 * 60 * 1000 + 1);
    let resultado = await escenario.servicio.restablecerContrasena({
      correo: 'cuenta.rnf03@evohka.invalid',
      codigo,
      nuevaContrasena: 'valor-sintetico-nuevo'
    });
    assert.equal(resultado.estado, ESTADO_INVALIDO);

    registro.fecha_expiracion = new Date('2026-11-11T12:00:00.000Z');
    registro.fecha_revocacion = new Date();
    resultado = await escenario.servicio.restablecerContrasena({
      correo: 'cuenta.rnf03@evohka.invalid',
      codigo,
      nuevaContrasena: 'valor-sintetico-nuevo'
    });
    assert.equal(resultado.estado, ESTADO_INVALIDO);

    registro.fecha_revocacion = null;
    registro.fecha_envio = null;
    resultado = await escenario.servicio.restablecerContrasena({
      correo: 'cuenta.rnf03@evohka.invalid',
      codigo,
      nuevaContrasena: 'valor-sintetico-nuevo'
    });
    assert.equal(resultado.estado, ESTADO_INVALIDO);
  });

  await t.test('restablece con bcrypt, usa el código y revoca sesiones', async () => {
    const costoOriginal = process.env.EVOHKA_BCRYPT_COSTO;
    process.env.EVOHKA_BCRYPT_COSTO = '10';
    try {
      const escenario = crearEscenario();
      await escenario.servicio.solicitarRecuperacion(
        'cuenta.rnf03@evohka.invalid'
      );
      const codigo = escenario.mensajes[0].text.match(/\b\d{8}\b/)[0];
      const resultado = await escenario.servicio.restablecerContrasena({
        correo: 'cuenta.rnf03@evohka.invalid',
        codigo,
        nuevaContrasena: 'valor-sintetico-nuevo'
      });

      assert.equal(resultado.estado, ESTADO_EXITOSO);
      const usuario = escenario.repository.estado.usuarios[0];
      assert.equal(usuario.contrasena_algoritmo, 'bcrypt');
      assert.equal(usuario.sesion_version, 5);
      assert.equal(
        await bcrypt.compare('valor-sintetico-nuevo', usuario.contrasena),
        true
      );
      assert.ok(
        escenario.repository.estado.recuperaciones[0].fecha_uso instanceof Date
      );

      const reutilizacion = await escenario.servicio.restablecerContrasena({
        correo: 'cuenta.rnf03@evohka.invalid',
        codigo,
        nuevaContrasena: 'otro-valor-sintetico'
      });
      assert.equal(reutilizacion.estado, ESTADO_INVALIDO);
      assert.equal(usuario.sesion_version, 5);
    } finally {
      if (costoOriginal === undefined) delete process.env.EVOHKA_BCRYPT_COSTO;
      else process.env.EVOHKA_BCRYPT_COSTO = costoOriginal;
    }
  });

  await t.test('aplica límites en memoria por cuenta, IP y volumen global', () => {
    let instante = 1000;
    const limites = crearRecuperacionLimiteService({ ahora: () => instante });
    for (let indice = 0; indice < LIMITES.solicitudCuenta; indice += 1) {
      assert.equal(limites.consumirSolicitud({
        correo: 'misma@evohka.invalid',
        ip: '192.0.2.1'
      }), true);
    }
    assert.equal(limites.consumirSolicitud({
      correo: 'misma@evohka.invalid',
      ip: '192.0.2.1'
    }), false);

    const limitesIp = crearRecuperacionLimiteService({ ahora: () => instante });
    for (let indice = 0; indice < LIMITES.solicitudIp; indice += 1) {
      assert.equal(limitesIp.consumirSolicitud({
        correo: `cuenta-${indice}@evohka.invalid`,
        ip: '192.0.2.2'
      }), true);
    }
    assert.equal(limitesIp.consumirSolicitud({
      correo: 'otra@evohka.invalid',
      ip: '192.0.2.2'
    }), false);

    const limitesGlobal = crearRecuperacionLimiteService({ ahora: () => instante });
    for (let indice = 0; indice < LIMITES.solicitudGlobal; indice += 1) {
      assert.equal(limitesGlobal.consumirSolicitud({
        correo: `global-${indice}@evohka.invalid`,
        ip: `198.51.100.${indice}`
      }), true);
    }
    assert.equal(limitesGlobal.consumirSolicitud({
      correo: 'exceso@evohka.invalid',
      ip: '203.0.113.1'
    }), false);
    instante += 16 * 60 * 1000;
    assert.equal(limitesGlobal.consumirSolicitud({
      correo: 'despues@evohka.invalid',
      ip: '203.0.113.1'
    }), true);
  });

  await t.test('solicitar responde igual para cualquier correo y registra las rutas', async () => {
    const programadas = [];
    const servicio = {
      verificarConfiguracion() {},
      async solicitarRecuperacion() {
        return { estado: 'sin_cuenta' };
      }
    };
    const controller = crearRecuperacionController({
      servicio,
      limites: {
        consumirSolicitud: () => true,
        consumirRestablecimiento: () => true
      },
      programar: (operacion) => programadas.push(operacion),
      registrarError() {}
    });

    const respuestas = [];
    for (const correo of [
      'cuenta.rnf03@evohka.invalid',
      'inexistente@evohka.invalid',
      'formato-invalido'
    ]) {
      const res = respuesta();
      await controller.solicitar({ body: { correo }, ip: '192.0.2.5' }, res);
      respuestas.push(res);
    }
    assert.deepEqual(
      respuestas.map((res) => [res.statusCode, res.body.mensaje]),
      Array(3).fill([202, MENSAJE_NEUTRAL])
    );
    assert.equal(programadas.length, 2);

    for (const ruta of [
      '/recuperacion/solicitar',
      '/recuperacion/restablecer'
    ]) {
      const capa = authRoutes.stack.find((actual) => actual.route?.path === ruta);
      assert.ok(capa);
      assert.equal(capa.route.methods.post, true);
    }
  });
});

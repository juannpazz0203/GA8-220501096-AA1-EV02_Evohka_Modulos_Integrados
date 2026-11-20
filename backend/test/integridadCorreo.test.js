const test = require('node:test');
const assert = require('node:assert/strict');
const { pool } = require('../src/config/db');
const authService = require('../src/services/authService');
const adminService = require('../src/services/adminService');
const authController = require('../src/controllers/authController');
const {
  LONGITUD_MAXIMA_CORREO,
  normalizarCorreo,
  correoValido
} = require('../src/utils/correo');

const CONTRASENA_PRUEBA = 'Clave-Sintetica-1!';

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

function datosRegistro(cambios = {}) {
  return {
    nombre: '  Nombre ',
    apellido: ' Apellido  ',
    correo: '  Persona.Prueba@EVOHKA.Local ',
    contrasena: CONTRASENA_PRUEBA,
    telefono: ' 3001234567 ',
    rol: 'inquilino',
    ...cambios
  };
}

function errorDuplicado() {
  const error = new Error('detalle interno que no debe exponerse');
  error.code = 'ER_DUP_ENTRY';
  error.errno = 1062;
  return error;
}

test('normalización y validación central del correo', async (t) => {
  await t.test('normaliza espacios exteriores y mayúsculas', () => {
    assert.equal(
      normalizarCorreo('  Persona.Prueba@EVOHKA.Local '),
      'persona.prueba@evohka.local'
    );
    assert.equal(normalizarCorreo(null), '');
    assert.equal(normalizarCorreo({ correo: 'objeto@evohka.local' }), '');
  });

  await t.test('valida formato, espacios y límite del esquema', () => {
    assert.equal(correoValido('persona@evohka.local'), true);
    assert.equal(correoValido('persona sin@evohka.local'), false);
    assert.equal(correoValido('persona@evohka'), false);
    assert.equal(
      correoValido(`${'a'.repeat(LONGITUD_MAXIMA_CORREO)}@x.co`),
      false
    );
  });
});

test('validaciones del registro público', async (t) => {
  const originales = {
    buscarUsuarioPorCorreo: authService.buscarUsuarioPorCorreo,
    registrarUsuario: authService.registrarUsuario
  };

  try {
    await t.test('conserva el contrato Android y normaliza inquilino', async () => {
      let recibidos;
      authService.buscarUsuarioPorCorreo = async () => false;
      authService.registrarUsuario = async (datos) => {
        recibidos = datos;
        return { id_usuario: 50, ...datos };
      };

      const res = respuesta();
      await authController.registro({ body: datosRegistro() }, res);

      assert.equal(res.statusCode, 201);
      assert.equal(res.body.ok, true);
      assert.equal(recibidos.nombre, 'Nombre');
      assert.equal(recibidos.apellido, 'Apellido');
      assert.equal(recibidos.correo, 'persona.prueba@evohka.local');
      assert.equal(recibidos.telefono, '3001234567');
      assert.equal(recibidos.rol, 'arrendatario');
    });

    await t.test('rechaza administrador y conserva propietario', async () => {
      const administrador = respuesta();
      await authController.registro({
        body: datosRegistro({ rol: 'administrador' })
      }, administrador);
      assert.equal(administrador.statusCode, 400);

      let rolRecibido;
      authService.buscarUsuarioPorCorreo = async () => false;
      authService.registrarUsuario = async (datos) => {
        rolRecibido = datos.rol;
        return { id_usuario: 51, ...datos };
      };
      const propietario = respuesta();
      await authController.registro({
        body: datosRegistro({ rol: 'propietario' })
      }, propietario);
      assert.equal(propietario.statusCode, 201);
      assert.equal(rolRecibido, 'propietario');
    });

    await t.test('rechaza tipos y longitudes incompatibles con MySQL', async () => {
      const casos = [
        { nombre: { valor: 'Nombre' } },
        { apellido: 'a'.repeat(101) },
        { correo: { valor: 'persona@evohka.local' } },
        { correo: 'formato-invalido' },
        { telefono: 3001234567 },
        { telefono: '1'.repeat(21) },
        { rol: ['arrendatario'] }
      ];

      for (const cambios of casos) {
        const res = respuesta();
        await authController.registro({
          body: datosRegistro(cambios)
        }, res);
        assert.equal(res.statusCode, 400);
        assert.equal(res.body.ok, false);
      }
    });

    await t.test('mantiene 409 para el prechequeo de correo', async () => {
      authService.buscarUsuarioPorCorreo = async () => true;
      const res = respuesta();
      await authController.registro({ body: datosRegistro() }, res);
      assert.equal(res.statusCode, 409);
      assert.equal(res.body.ok, false);
    });

    await t.test('convierte la colisión del INSERT en 409 genérico', async () => {
      authService.buscarUsuarioPorCorreo = async () => false;
      authService.registrarUsuario = async () => ({
        error: 'correo_duplicado'
      });
      const res = respuesta();
      await authController.registro({ body: datosRegistro() }, res);
      assert.equal(res.statusCode, 409);
      assert.equal(res.body.ok, false);
      assert.doesNotMatch(res.body.mensaje, /ER_DUP_ENTRY|1062|SQL/i);
    });
  } finally {
    Object.assign(authService, originales);
  }
});

test('traducción de ER_DUP_ENTRY en los servicios de usuarios', async (t) => {
  const executeOriginal = pool.execute;
  const getConnectionOriginal = pool.getConnection;
  const costoOriginal = process.env.EVOHKA_BCRYPT_COSTO;
  process.env.EVOHKA_BCRYPT_COSTO = '10';

  try {
    await t.test('registro público devuelve error semántico', async () => {
      pool.execute = async () => {
        throw errorDuplicado();
      };
      const resultado = await authService.registrarUsuario({
        ...datosRegistro(),
        correo: 'persona@evohka.local',
        rol: 'arrendatario'
      });
      assert.deepEqual(resultado, { error: 'correo_duplicado' });
    });

    await t.test('alta administrativa devuelve error semántico', async () => {
      let paso = 0;
      pool.execute = async () => {
        paso += 1;
        if (paso === 1) return [[]];
        throw errorDuplicado();
      };
      const resultado = await adminService.crearUsuario({
        ...datosRegistro(),
        correo: 'persona@evohka.local',
        rol: 'administrador'
      });
      assert.deepEqual(resultado, { error: 'correo_duplicado' });
      assert.equal(paso, 2);
    });

    await t.test('edición administrativa revierte y devuelve error semántico', async () => {
      let paso = 0;
      let reversiones = 0;
      let liberada = false;
      pool.getConnection = async () => ({
        async beginTransaction() {},
        async commit() {},
        async rollback() {
          reversiones += 1;
        },
        release() {
          liberada = true;
        },
        async execute() {
          paso += 1;
          if (paso === 1) {
            return [[{ id_usuario: 80, rol: 'arrendatario' }]];
          }
          if (paso === 2) return [[]];
          throw errorDuplicado();
        }
      });

      const resultado = await adminService.actualizarUsuario(80, 1, {
        nombre: 'Nombre',
        apellido: 'Apellido',
        correo: 'persona@evohka.local',
        telefono: null,
        rol: 'arrendatario'
      });
      assert.deepEqual(resultado, { error: 'correo_duplicado' });
      assert.equal(reversiones, 1);
      assert.equal(liberada, true);
    });
  } finally {
    pool.execute = executeOriginal;
    pool.getConnection = getConnectionOriginal;
    if (costoOriginal === undefined) {
      delete process.env.EVOHKA_BCRYPT_COSTO;
    } else {
      process.env.EVOHKA_BCRYPT_COSTO = costoOriginal;
    }
  }
});

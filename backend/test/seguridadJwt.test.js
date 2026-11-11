const test = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');
const { pool } = require('../src/config/db');
const {
  JWT_ALGORITMO,
  JWT_ISSUER,
  JWT_AUDIENCE,
  obtenerConfiguracionJwt
} = require('../src/config/seguridad');
const jwtService = require('../src/services/jwtService');
const sesionService = require('../src/services/sesionService');
const authService = require('../src/services/authService');
const adminService = require('../src/services/adminService');
const { verificarToken } = require('../src/middlewares/authMiddleware');
const authController = require('../src/controllers/authController');
const authRoutes = require('../src/routes/authRoutes');

const SECRETO_PRUEBA =
  'secreto-sintetico-rnf03-jwt-32-bytes-minimo';

function usuario(overrides = {}) {
  return {
    id_usuario: 501,
    nombre: 'Usuario',
    apellido: 'Sintetico',
    correo: 'jwt.rnf03@evohka.local',
    telefono: null,
    rol: 'propietario',
    sesion_version: 0,
    ...overrides
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

async function autenticar(token) {
  const req = {
    headers: { authorization: `Bearer ${token}` }
  };
  const res = respuesta();
  let siguiente = false;

  await verificarToken(req, res, () => {
    siguiente = true;
  });

  return { req, res, siguiente };
}

function tokenActual(payload = {}, opciones = {}) {
  const datos = usuario(payload);
  return jwt.sign(
    {
      id_usuario: datos.id_usuario,
      correo: datos.correo,
      rol: datos.rol,
      sv: datos.sesion_version
    },
    SECRETO_PRUEBA,
    {
      algorithm: JWT_ALGORITMO,
      issuer: JWT_ISSUER,
      audience: JWT_AUDIENCE,
      subject: String(datos.id_usuario),
      expiresIn: '2h',
      ...opciones
    }
  );
}

test('seguridad JWT y sesiones versionadas', { concurrency: false }, async (t) => {
  const executeOriginal = pool.execute;
  const getConnectionOriginal = pool.getConnection;
  const validarCredencialesOriginal = authService.validarCredenciales;
  const variables = [
    'JWT_SECRET',
    'EVOHKA_SESIONES_VERSIONADAS_ACTIVAS',
    'EVOHKA_JWT_TRANSICION_LEGACY_ACTIVA',
    'EVOHKA_JWT_TRANSICION_LEGACY_HASTA'
  ];
  const entornoOriginal = Object.fromEntries(
    variables.map((nombre) => [nombre, process.env[nombre]])
  );

  process.env.JWT_SECRET = SECRETO_PRUEBA;
  process.env.EVOHKA_SESIONES_VERSIONADAS_ACTIVAS = 'true';
  delete process.env.EVOHKA_JWT_TRANSICION_LEGACY_ACTIVA;
  delete process.env.EVOHKA_JWT_TRANSICION_LEGACY_HASTA;

  try {
    await t.test('rechaza un secreto JWT insuficiente', () => {
      process.env.JWT_SECRET = 'corto';
      assert.throws(
        () => obtenerConfiguracionJwt(),
        (error) => error.code === 'CONFIGURACION_JWT_INVALIDA'
      );
      process.env.JWT_SECRET = SECRETO_PRUEBA;
    });

    await t.test('emite y verifica JWT actual con claims obligatorios', () => {
      const token = jwtService.crearToken(usuario());
      const verificado = jwtService.verificarToken(token);
      assert.equal(verificado.formato, 'actual');
      assert.equal(verificado.id_usuario, 501);
      assert.equal(verificado.sesion_version, 0);
      assert.equal(verificado.payload.iss, JWT_ISSUER);
      assert.equal(verificado.payload.aud, JWT_AUDIENCE);
      assert.equal(verificado.payload.sub, '501');
    });

    await t.test('el login conserva Bearer y no expone la versión de sesión', async () => {
      authService.validarCredenciales = async () => usuario();
      const req = {
        body: {
          correo: 'jwt.rnf03@evohka.local',
          contrasena: 'valor-sintetico-no-real'
        }
      };
      const res = respuesta();
      await authController.login(req, res);
      assert.equal(res.statusCode, 200);
      assert.match(res.body.token, /^Bearer /);
      assert.equal(res.body.usuario.id_usuario, 501);
      assert.equal(res.body.usuario.sesion_version, undefined);
      authService.validarCredenciales = validarCredencialesOriginal;
    });

    await t.test('rechaza un JWT alterado', () => {
      const partes = jwtService.crearToken(usuario()).split('.');
      partes[2] = `${partes[2][0] === 'a' ? 'b' : 'a'}${partes[2].slice(1)}`;
      assert.throws(() => jwtService.verificarToken(partes.join('.')));
    });

    await t.test('rechaza un JWT vencido', () => {
      const token = tokenActual({}, { expiresIn: -1 });
      assert.throws(
        () => jwtService.verificarToken(token),
        (error) => error.name === 'TokenExpiredError'
      );
    });

    await t.test('rechaza issuer y audience incorrectos', () => {
      const issuerIncorrecto = tokenActual({}, { issuer: 'otro-servicio' });
      const audienceIncorrecta = tokenActual({}, { audience: 'otro-cliente' });
      assert.throws(() => jwtService.verificarToken(issuerIncorrecto));
      assert.throws(() => jwtService.verificarToken(audienceIncorrecta));
    });

    await t.test('rechaza un usuario eliminado', async () => {
      pool.execute = async () => [[]];
      const resultado = await autenticar(jwtService.crearToken(usuario()));
      assert.equal(resultado.siguiente, false);
      assert.equal(resultado.res.statusCode, 401);
      assert.equal(resultado.res.body.mensaje, 'Sesión inválida o revocada');
    });

    await t.test('usa el rol actual y no el rol incluido en el token', async () => {
      pool.execute = async () => [[usuario({ rol: 'arrendatario' })]];
      const token = jwtService.crearToken(usuario({ rol: 'administrador' }));
      const resultado = await autenticar(token);
      assert.equal(resultado.siguiente, true);
      assert.equal(resultado.req.usuario.rol, 'arrendatario');
    });

    await t.test('rechaza una versión de sesión revocada', async () => {
      pool.execute = async () => [[usuario({ sesion_version: 1 })]];
      const resultado = await autenticar(
        jwtService.crearToken(usuario({ sesion_version: 0 }))
      );
      assert.equal(resultado.siguiente, false);
      assert.equal(resultado.res.statusCode, 401);
    });

    await t.test('GET /api/auth/sesion devuelve identidad actual sin sv', async () => {
      pool.execute = async () => [[usuario({ rol: 'arrendatario' })]];
      const autenticacion = await autenticar(
        jwtService.crearToken(usuario({ rol: 'propietario' }))
      );
      assert.equal(autenticacion.siguiente, true);

      const res = respuesta();
      await authController.sesion(autenticacion.req, res);
      assert.equal(res.statusCode, 200);
      assert.equal(res.body.ok, true);
      assert.equal(res.body.usuario.rol, 'arrendatario');
      assert.equal(res.body.usuario.sesion_version, undefined);

      const ruta = authRoutes.stack.find(
        (capa) => capa.route?.path === '/sesion'
      );
      assert.ok(ruta);
      assert.equal(ruta.route.methods.get, true);
      assert.equal(ruta.route.stack.length, 2);
    });

    await t.test('acepta JWT legacy solo durante la ventana y con sv actual cero', async () => {
      process.env.EVOHKA_JWT_TRANSICION_LEGACY_ACTIVA = 'true';
      process.env.EVOHKA_JWT_TRANSICION_LEGACY_HASTA =
        new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString();
      const token = jwt.sign(
        {
          id_usuario: 501,
          correo: 'jwt.rnf03@evohka.local',
          rol: 'administrador'
        },
        SECRETO_PRUEBA,
        { algorithm: JWT_ALGORITMO, expiresIn: '90m' }
      );
      const tokenConIssuerNoPermitido = jwt.sign(
        {
          id_usuario: 501,
          correo: 'jwt.rnf03@evohka.local',
          rol: 'administrador'
        },
        SECRETO_PRUEBA,
        {
          algorithm: JWT_ALGORITMO,
          expiresIn: '90m',
          issuer: 'issuer-no-autorizado'
        }
      );

      assert.throws(
        () => jwtService.verificarToken(tokenConIssuerNoPermitido)
      );

      pool.execute = async () => [[usuario({ rol: 'propietario' })]];
      let resultado = await autenticar(token);
      assert.equal(resultado.siguiente, true);
      assert.equal(resultado.req.autenticacion.formatoToken, 'legacy');
      assert.equal(resultado.req.usuario.rol, 'propietario');

      pool.execute = async () => [[usuario({ sesion_version: 1 })]];
      resultado = await autenticar(token);
      assert.equal(resultado.siguiente, false);
      assert.equal(resultado.res.statusCode, 401);

      delete process.env.EVOHKA_JWT_TRANSICION_LEGACY_ACTIVA;
      delete process.env.EVOHKA_JWT_TRANSICION_LEGACY_HASTA;
    });

    await t.test('no consulta sesion_version antes de activar la migración', async () => {
      process.env.EVOHKA_SESIONES_VERSIONADAS_ACTIVAS = 'false';
      pool.execute = async (sql) => {
        assert.doesNotMatch(sql, /sesion_version/);
        return [[usuario({ sesion_version: undefined })]];
      };
      const actual = await sesionService.obtenerUsuarioAutorizacion(501);
      assert.equal(actual.sesion_version, 0);
      process.env.EVOHKA_SESIONES_VERSIONADAS_ACTIVAS = 'true';
    });

    await t.test('la revocación incrementa la versión solo con esquema activo', async () => {
      let actualizaciones = 0;
      const ejecutor = {
        execute: async (sql, parametros) => {
          actualizaciones += 1;
          assert.match(sql, /sesion_version = sesion_version \+ 1/);
          assert.deepEqual(parametros, [501]);
          return [{ affectedRows: 1 }];
        }
      };

      let resultado = await sesionService.revocarSesionesUsuario(501, ejecutor);
      assert.equal(resultado.actualizada, true);
      assert.equal(actualizaciones, 1);

      process.env.EVOHKA_SESIONES_VERSIONADAS_ACTIVAS = 'false';
      resultado = await sesionService.revocarSesionesUsuario(501, ejecutor);
      assert.equal(resultado.activada, false);
      assert.equal(actualizaciones, 1);
      process.env.EVOHKA_SESIONES_VERSIONADAS_ACTIVAS = 'true';
    });

    await t.test('un cambio administrativo de rol revoca sesiones', async () => {
      let paso = 0;
      const conexion = {
        beginTransaction: async () => {},
        commit: async () => {},
        rollback: async () => {},
        release: () => {},
        execute: async (sql) => {
          paso += 1;
          if (paso === 1) return [[{ id_usuario: 501, rol: 'arrendatario' }]];
          if (paso === 2) return [[]];
          if (paso === 3) {
            assert.doesNotMatch(sql, /sesion_version/);
            return [{ affectedRows: 1 }];
          }
          if (paso === 4) {
            assert.match(sql, /sesion_version = sesion_version \+ 1/);
            return [{ affectedRows: 1 }];
          }
          return [[usuario({ rol: 'propietario' })]];
        }
      };
      pool.getConnection = async () => conexion;

      const resultado = await adminService.actualizarUsuario(
        501,
        999,
        {
          nombre: 'Usuario',
          apellido: 'Sintetico',
          correo: 'jwt.rnf03@evohka.local',
          telefono: null,
          rol: 'propietario'
        }
      );
      assert.equal(resultado.datos.rol, 'propietario');
      assert.equal(paso, 5);
    });
  } finally {
    pool.execute = executeOriginal;
    pool.getConnection = getConnectionOriginal;
    authService.validarCredenciales = validarCredencialesOriginal;
    for (const nombre of variables) {
      if (entornoOriginal[nombre] === undefined) {
        delete process.env[nombre];
      } else {
        process.env[nombre] = entornoOriginal[nombre];
      }
    }
  }
});

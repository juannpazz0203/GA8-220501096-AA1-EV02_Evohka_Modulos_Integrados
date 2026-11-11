const test = require('node:test');
const assert = require('node:assert/strict');
const { pool, probarConexion } = require('../src/config/db');
const {
  ERROR_ESQUEMA_CREDENCIALES,
  verificarEsquemaCredenciales
} = require('../src/config/credenciales');
const contrasenaService = require('../src/services/contrasenaService');
const authService = require('../src/services/authService');
const adminService = require('../src/services/adminService');

const CONTRASENA_PRUEBA = 'Clave-Sintetica-1!';
const CORREO_PRUEBA = 'bcrypt.prueba@evohka.local';
const JWT_SECRETO_PRUEBA =
  'secreto-sintetico-rnf03-credenciales-32-bytes';

function usuario(cambios = {}) {
  return {
    id_usuario: 501,
    nombre: 'Usuario',
    apellido: 'Sintetico',
    correo: CORREO_PRUEBA,
    contrasena: CONTRASENA_PRUEBA,
    contrasena_algoritmo: contrasenaService.ALGORITMO_LEGACY,
    telefono: null,
    rol: 'arrendatario',
    ...cambios
  };
}

test('protección y migración de contraseñas RNF03', async (t) => {
  const executeOriginal = pool.execute;
  const costoOriginal = process.env.EVOHKA_BCRYPT_COSTO;
  process.env.EVOHKA_BCRYPT_COSTO = '10';

  try {
    await t.test('acepta el esquema obligatorio de credenciales', async () => {
      let consulta;
      await verificarEsquemaCredenciales({
        execute: async (sql) => {
          consulta = sql;
          return [[{ columnas_compatibles: 1 }]];
        }
      });
      assert.match(consulta, /contrasena_algoritmo/);
      assert.match(consulta, /information_schema\.columns/);
    });

    await t.test('rechaza el arranque si falta la columna obligatoria', async () => {
      await assert.rejects(
        verificarEsquemaCredenciales({
          execute: async () => [[{ columnas_compatibles: 0 }]]
        }),
        (error) => {
          assert.equal(error.code, ERROR_ESQUEMA_CREDENCIALES);
          assert.match(error.message, /esquema RNF03 es obligatorio/);
          return true;
        }
      );
    });

    await t.test('el arranque libera la conexión y propaga el error de esquema', async () => {
      const obtenerConexionOriginal = pool.getConnection;
      const jwtSecretOriginal = process.env.JWT_SECRET;
      let liberada = false;
      process.env.JWT_SECRET = JWT_SECRETO_PRUEBA;
      pool.getConnection = async () => ({
        execute: async () => [[{ columnas_compatibles: 0 }]],
        release: () => {
          liberada = true;
        }
      });

      try {
        await assert.rejects(
          probarConexion(),
          (error) => error.code === ERROR_ESQUEMA_CREDENCIALES
        );
        assert.equal(liberada, true);
      } finally {
        pool.getConnection = obtenerConexionOriginal;
        if (jwtSecretOriginal === undefined) {
          delete process.env.JWT_SECRET;
        } else {
          process.env.JWT_SECRET = jwtSecretOriginal;
        }
      }
    });

    await t.test('acepta una contraseña correcta con bcrypt', async () => {
      const hash = await contrasenaService.crearHash(CONTRASENA_PRUEBA);
      assert.equal(
        await contrasenaService.verificarContrasena(
          CONTRASENA_PRUEBA,
          hash,
          contrasenaService.ALGORITMO_BCRYPT
        ),
        true
      );
    });

    await t.test('rechaza una contraseña incorrecta con bcrypt', async () => {
      const hash = await contrasenaService.crearHash(CONTRASENA_PRUEBA);
      assert.equal(
        await contrasenaService.verificarContrasena(
          'Clave-Sintetica-Incorrecta-2!',
          hash,
          contrasenaService.ALGORITMO_BCRYPT
        ),
        false
      );
    });

    await t.test('valida una cuenta legacy y la migra oportunistamente', async () => {
      let llamadas = 0;
      pool.execute = async (sql, parametros) => {
        llamadas += 1;
        if (sql.includes('FROM usuarios') && sql.includes('WHERE correo')) {
          return [[usuario()]];
        }
        if (sql.includes('UPDATE usuarios')) {
          assert.notEqual(parametros[0], CONTRASENA_PRUEBA);
          assert.equal(
            parametros[1],
            contrasenaService.ALGORITMO_BCRYPT
          );
          assert.equal(parametros[3], CONTRASENA_PRUEBA);
          return [{ affectedRows: 1 }];
        }
        throw new Error('Consulta no esperada en prueba legacy');
      };

      const autenticado = await authService.validarCredenciales(
        CORREO_PRUEBA,
        CONTRASENA_PRUEBA
      );
      assert.equal(autenticado.id_usuario, 501);
      assert.equal(autenticado.contrasena, undefined);
      assert.equal(llamadas, 2);
    });

    await t.test('normaliza un bcrypt marcado una sola vez', async () => {
      const hashLimpio = await contrasenaService.crearHash(
        CONTRASENA_PRUEBA
      );
      const hashMarcado =
        contrasenaService.envolverHashParaEsquemaLegacy(hashLimpio);
      let fila = usuario({ contrasena: hashMarcado });
      let actualizaciones = 0;

      pool.execute = async (sql, parametros) => {
        if (sql.includes('FROM usuarios') && sql.includes('WHERE correo')) {
          return [[{ ...fila }]];
        }
        if (sql.includes('UPDATE usuarios')) {
          actualizaciones += 1;
          assert.equal(parametros[0], hashLimpio);
          assert.equal(
            parametros[1],
            contrasenaService.ALGORITMO_BCRYPT
          );
          assert.equal(parametros[3], hashMarcado);
          assert.equal(
            parametros[4],
            contrasenaService.ALGORITMO_LEGACY
          );
          fila = {
            ...fila,
            contrasena: parametros[0],
            contrasena_algoritmo: parametros[1]
          };
          return [{ affectedRows: 1 }];
        }
        throw new Error('Consulta no esperada en prueba de marcador');
      };

      const primeraAutenticacion = await authService.validarCredenciales(
        CORREO_PRUEBA,
        CONTRASENA_PRUEBA
      );
      const segundaAutenticacion = await authService.validarCredenciales(
        CORREO_PRUEBA,
        CONTRASENA_PRUEBA
      );

      assert.equal(primeraAutenticacion.id_usuario, 501);
      assert.equal(segundaAutenticacion.id_usuario, 501);
      assert.equal(fila.contrasena, hashLimpio);
      assert.equal(
        fila.contrasena_algoritmo,
        contrasenaService.ALGORITMO_BCRYPT
      );
      assert.equal(actualizaciones, 1);
    });

    await t.test('valida una cuenta ya migrada sin actualizarla', async () => {
      const hash = await contrasenaService.crearHash(CONTRASENA_PRUEBA);
      let llamadas = 0;
      pool.execute = async (sql) => {
        llamadas += 1;
        assert.doesNotMatch(sql, /UPDATE usuarios/);
        return [[usuario({
          contrasena: hash,
          contrasena_algoritmo: contrasenaService.ALGORITMO_BCRYPT
        })]];
      };

      const autenticado = await authService.validarCredenciales(
        CORREO_PRUEBA,
        CONTRASENA_PRUEBA
      );
      assert.equal(autenticado.id_usuario, 501);
      assert.equal(llamadas, 1);
    });

    await t.test('el registro público almacena bcrypt explícito', async () => {
      let credencialGuardada;
      pool.execute = async (sql, parametros) => {
        assert.match(sql, /contrasena_algoritmo/);
        credencialGuardada = parametros[3];
        assert.equal(
          parametros[6],
          contrasenaService.ALGORITMO_BCRYPT
        );
        return [{ insertId: 502 }];
      };

      const creado = await authService.registrarUsuario({
        nombre: 'Registro',
        apellido: 'Sintetico',
        correo: CORREO_PRUEBA,
        contrasena: CONTRASENA_PRUEBA,
        telefono: null,
        rol: 'arrendatario'
      });
      assert.notEqual(credencialGuardada, CONTRASENA_PRUEBA);
      assert.equal(
        await contrasenaService.verificarContrasena(
          CONTRASENA_PRUEBA,
          credencialGuardada,
          contrasenaService.ALGORITMO_BCRYPT
        ),
        true
      );
      assert.equal(creado.contrasena, undefined);
    });

    await t.test('el alta administrativa almacena bcrypt explícito', async () => {
      let paso = 0;
      pool.execute = async (sql, parametros = []) => {
        paso += 1;
        if (paso === 1) return [[]];
        if (paso === 2) {
          assert.match(sql, /contrasena_algoritmo/);
          assert.notEqual(parametros[3], CONTRASENA_PRUEBA);
          assert.equal(
            parametros[6],
            contrasenaService.ALGORITMO_BCRYPT
          );
          return [{ insertId: 503 }];
        }
        return [[{
          id_usuario: 503,
          nombre: 'Alta',
          apellido: 'Administrativa',
          correo: CORREO_PRUEBA,
          telefono: null,
          rol: 'propietario'
        }]];
      };

      const resultado = await adminService.crearUsuario({
        nombre: 'Alta',
        apellido: 'Administrativa',
        correo: CORREO_PRUEBA,
        contrasena: CONTRASENA_PRUEBA,
        telefono: null,
        rol: 'propietario'
      });
      assert.equal(resultado.datos.id_usuario, 503);
      assert.equal(resultado.datos.contrasena, undefined);
      assert.equal(paso, 3);
    });

    await t.test('rechaza la credencial antigua tras un cambio concurrente', async () => {
      const hashNuevo = await contrasenaService.crearHash(
        'Clave-Sintetica-Nueva-3!'
      );
      let paso = 0;
      pool.execute = async () => {
        paso += 1;
        if (paso === 1) return [[usuario()]];
        if (paso === 2) return [{ affectedRows: 0 }];
        return [[{
          contrasena: hashNuevo,
          contrasena_algoritmo: contrasenaService.ALGORITMO_BCRYPT
        }]];
      };

      const autenticado = await authService.validarCredenciales(
        CORREO_PRUEBA,
        CONTRASENA_PRUEBA
      );
      assert.equal(autenticado, null);
      assert.equal(paso, 3);
    });
  } finally {
    pool.execute = executeOriginal;
    if (costoOriginal === undefined) {
      delete process.env.EVOHKA_BCRYPT_COSTO;
    } else {
      process.env.EVOHKA_BCRYPT_COSTO = costoOriginal;
    }
  }
});

const test = require('node:test');
const assert = require('node:assert/strict');
const adminService = require('../src/services/adminService');
const adminController = require('../src/controllers/adminController');

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

function solicitud(rol = 'administrador', cambios = {}) {
  return {
    usuario: { id_usuario: 1, rol },
    params: {},
    body: {},
    ...cambios
  };
}

test('autorización y gestión administrativa', async (t) => {
  const originales = {
    listarUsuarios: adminService.listarUsuarios,
    crearUsuario: adminService.crearUsuario,
    actualizarUsuario: adminService.actualizarUsuario,
    eliminarUsuario: adminService.eliminarUsuario
  };

  try {
    await t.test('propietario y arrendatario reciben 403', async () => {
      for (const rol of ['propietario', 'arrendatario']) {
        const res = respuesta();
        await adminController.listarUsuarios(solicitud(rol), res);
        assert.equal(res.statusCode, 403);
      }
    });

    await t.test('administrador lista usuarios', async () => {
      adminService.listarUsuarios = async () => [{ id_usuario: 1 }];
      const res = respuesta();
      await adminController.listarUsuarios(solicitud(), res);
      assert.equal(res.statusCode, 200);
      assert.equal(res.body.total, 1);
    });

    await t.test('crea los tres roles permitidos', async () => {
      adminService.crearUsuario = async (datos) => ({
        datos: { id_usuario: 2, ...datos }
      });
      for (const rol of ['arrendatario', 'propietario', 'administrador']) {
        const res = respuesta();
        await adminController.crearUsuario(
          solicitud('administrador', {
            body: {
              nombre: 'Temporal',
              apellido: 'Prueba',
              correo: `${rol}@evohka.local`,
              contrasena: 'prueba123',
              rol
            }
          }),
          res
        );
        assert.equal(res.statusCode, 201);
        assert.equal(res.body.datos.rol, rol);
      }
    });

    await t.test('rechaza rol inválido', async () => {
      const res = respuesta();
      await adminController.crearUsuario(
        solicitud('administrador', {
          body: {
            nombre: 'Temporal',
            apellido: 'Prueba',
            correo: 'temporal@evohka.local',
            contrasena: 'prueba123',
            rol: 'superusuario'
          }
        }),
        res
      );
      assert.equal(res.statusCode, 400);
    });

    await t.test('correo duplicado devuelve 409', async () => {
      adminService.crearUsuario = async () => ({ error: 'correo_duplicado' });
      const res = respuesta();
      await adminController.crearUsuario(
        solicitud('administrador', {
          body: {
            nombre: 'Temporal',
            apellido: 'Prueba',
            correo: 'duplicado@evohka.local',
            contrasena: 'prueba123',
            rol: 'arrendatario'
          }
        }),
        res
      );
      assert.equal(res.statusCode, 409);
    });

    await t.test('edita datos y cambia rol', async () => {
      adminService.actualizarUsuario = async (id, actor, datos) => ({
        datos: { id_usuario: id, ...datos }
      });
      const res = respuesta();
      await adminController.actualizarUsuario(
        solicitud('administrador', {
          params: { id: '2' },
          body: {
            nombre: 'Nombre editado',
            apellido: 'Prueba',
            correo: 'editado@evohka.local',
            rol: 'propietario'
          }
        }),
        res
      );
      assert.equal(res.statusCode, 200);
      assert.equal(res.body.datos.rol, 'propietario');
    });

    await t.test('protege último administrador al editar y eliminar', async () => {
      adminService.actualizarUsuario = async () => ({ error: 'ultimo_administrador' });
      let res = respuesta();
      await adminController.actualizarUsuario(
        solicitud('administrador', {
          params: { id: '2' },
          body: {
            nombre: 'Admin',
            apellido: 'Prueba',
            correo: 'admin@evohka.local',
            rol: 'propietario'
          }
        }),
        res
      );
      assert.equal(res.statusCode, 409);

      adminService.eliminarUsuario = async () => ({ error: 'ultimo_administrador' });
      res = respuesta();
      await adminController.eliminarUsuario(
        solicitud('administrador', { params: { id: '2' } }),
        res
      );
      assert.equal(res.statusCode, 409);
    });

    await t.test('elimina sin relaciones y rechaza con relaciones', async () => {
      adminService.eliminarUsuario = async () => ({ datos: { id_usuario: 2 } });
      let res = respuesta();
      await adminController.eliminarUsuario(
        solicitud('administrador', { params: { id: '2' } }),
        res
      );
      assert.equal(res.statusCode, 200);

      adminService.eliminarUsuario = async () => ({ error: 'tiene_relaciones' });
      res = respuesta();
      await adminController.eliminarUsuario(
        solicitud('administrador', { params: { id: '2' } }),
        res
      );
      assert.equal(res.statusCode, 409);
    });
  } finally {
    Object.assign(adminService, originales);
  }
});

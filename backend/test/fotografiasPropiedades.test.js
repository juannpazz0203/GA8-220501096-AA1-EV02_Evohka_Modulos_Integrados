const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs/promises');
const os = require('os');
const path = require('path');
const sharp = require('sharp');
const {
  MAX_BYTES_FOTOGRAFIA
} = require('../src/config/fotografias');
const {
  crearAlmacenamientoFotografias
} = require('../src/services/almacenamientoFotografiaService');
const {
  crearFotoPropiedadService
} = require('../src/services/fotoPropiedadService');
const {
  exigirPropietario
} = require('../src/controllers/fotoPropiedadController');
const { verificarToken } = require('../src/middlewares/authMiddleware');
const propiedadRoutes = require('../src/routes/propiedadRoutes');

function crearRepositorioSimulado() {
  const estado = {
    propiedades: new Map([
      [101, { id_propiedad: 101, id_propietario: 7 }]
    ]),
    fotografias: [],
    siguienteId: 1
  };

  const operaciones = {
    async obtenerPropiedad(idPropiedad) {
      return estado.propiedades.get(Number(idPropiedad)) || null;
    },
    async contarFotografias(idPropiedad) {
      return estado.fotografias.filter(
        (foto) => foto.id_propiedad === Number(idPropiedad)
      ).length;
    },
    async obtenerMayorOrden(idPropiedad) {
      const ordenes = estado.fotografias
        .filter((foto) => foto.id_propiedad === Number(idPropiedad))
        .map((foto) => foto.orden);
      return ordenes.length === 0 ? -1 : Math.max(...ordenes);
    },
    async insertarFotografia(datos) {
      const idFoto = estado.siguienteId;
      estado.siguienteId += 1;
      estado.fotografias.push({
        id_foto: idFoto,
        id_propiedad: Number(datos.id_propiedad),
        ruta_relativa: datos.ruta_relativa,
        mime_type: datos.mime_type,
        tamano_bytes: Number(datos.tamano_bytes),
        orden: Number(datos.orden),
        fecha_creacion: new Date().toISOString()
      });
      return idFoto;
    },
    async listarFotografias(idPropiedad) {
      return estado.fotografias
        .filter((foto) => foto.id_propiedad === Number(idPropiedad))
        .sort((a, b) => a.orden - b.orden || a.id_foto - b.id_foto)
        .map((foto) => ({ ...foto }));
    },
    async obtenerFotografia(idPropiedad, idFoto) {
      const foto = estado.fotografias.find(
        (item) =>
          item.id_propiedad === Number(idPropiedad) &&
          item.id_foto === Number(idFoto)
      );
      return foto ? { ...foto } : null;
    },
    async eliminarFotografia(idPropiedad, idFoto) {
      const indice = estado.fotografias.findIndex(
        (item) =>
          item.id_propiedad === Number(idPropiedad) &&
          item.id_foto === Number(idFoto)
      );
      if (indice < 0) return false;
      estado.fotografias.splice(indice, 1);
      return true;
    },
    async desplazarOrdenTemporal(idPropiedad) {
      for (const foto of estado.fotografias) {
        if (foto.id_propiedad === Number(idPropiedad)) foto.orden += 100;
      }
    },
    async actualizarOrden(idPropiedad, idFoto, orden) {
      const foto = estado.fotografias.find(
        (item) =>
          item.id_propiedad === Number(idPropiedad) &&
          item.id_foto === Number(idFoto)
      );
      if (!foto) return false;
      foto.orden = Number(orden);
      return true;
    }
  };

  return {
    ...operaciones,
    estado,
    async conTransaccion(operacion) {
      const respaldo = {
        fotografias: estado.fotografias.map((foto) => ({ ...foto })),
        siguienteId: estado.siguienteId
      };
      try {
        return await operacion(operaciones);
      } catch (error) {
        estado.fotografias.splice(
          0,
          estado.fotografias.length,
          ...respaldo.fotografias
        );
        estado.siguienteId = respaldo.siguienteId;
        throw error;
      }
    }
  };
}

async function imagenJpegValida() {
  return sharp({
    create: {
      width: 80,
      height: 60,
      channels: 3,
      background: '#CE74FF'
    }
  })
    .jpeg()
    .toBuffer();
}

function archivo(buffer, mimetype = 'image/jpeg') {
  return {
    buffer,
    mimetype,
    originalname: 'nombre-no-utilizado.jpg'
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

test('fotografías de propiedades con MySQL simulado', { concurrency: false }, async (t) => {
  const temporal = await fs.mkdtemp(
    path.join(os.tmpdir(), 'evohka-fotos-')
  );
  const almacenamiento = crearAlmacenamientoFotografias({
    directorioRaiz: temporal
  });
  const repositorio = crearRepositorioSimulado();
  const servicio = crearFotoPropiedadService({
    repositorio,
    almacenamiento
  });
  const jpeg = await imagenJpegValida();

  try {
    await t.test('sube, valida y optimiza una fotografía a WebP', async () => {
      const creadas = await servicio.subirFotografias(
        101,
        7,
        [archivo(jpeg)]
      );

      assert.equal(creadas.length, 1);
      assert.equal(creadas[0].mime_type, 'image/webp');
      assert.equal(creadas[0].orden, 0);
      assert.equal(creadas[0].es_portada, true);
      assert.equal(
        creadas[0].ruta_http,
        `/api/propiedades/101/fotos/${creadas[0].id_foto}/archivo`
      );

      const registro = repositorio.estado.fotografias[0];
      const ruta = await almacenamiento.obtenerArchivo(
        registro.ruta_relativa
      );
      const contenidoOptimizado = await fs.readFile(ruta);
      const metadata = await sharp(contenidoOptimizado).metadata();
      assert.equal(metadata.format, 'webp');
      assert.ok(Math.max(metadata.width, metadata.height) <= 1920);
    });

    await t.test('rechaza contenido dañado aunque declare MIME permitido', async () => {
      await assert.rejects(
        servicio.subirFotografias(
          101,
          7,
          [archivo(Buffer.from('esto no es una imagen'))]
        ),
        (error) => error.codigo === 'ARCHIVO_INVALIDO'
      );
    });

    await t.test('rechaza originales mayores de 15 MB', async () => {
      await assert.rejects(
        almacenamiento.procesarImagen(
          archivo(Buffer.alloc(MAX_BYTES_FOTOGRAFIA + 1))
        ),
        (error) =>
          error.codigo === 'ARCHIVO_DEMASIADO_GRANDE' &&
          error.estadoHttp === 413
      );
    });

    await t.test('impide superar diez fotografías por propiedad', async () => {
      const repositorioLimite = crearRepositorioSimulado();
      const servicioLimite = crearFotoPropiedadService({
        repositorio: repositorioLimite,
        almacenamiento
      });

      await servicioLimite.subirFotografias(
        101,
        7,
        Array.from({ length: 10 }, () => archivo(jpeg))
      );
      await assert.rejects(
        servicioLimite.subirFotografias(101, 7, [archivo(jpeg)]),
        (error) => error.codigo === 'LIMITE_FOTOGRAFIAS'
      );
      assert.equal(repositorioLimite.estado.fotografias.length, 10);
    });

    await t.test('rechaza al propietario ajeno antes de procesar archivos', async () => {
      await assert.rejects(
        servicio.subirFotografias(101, 99, [archivo(jpeg)]),
        (error) =>
          error.codigo === 'SIN_PERMISO' && error.estadoHttp === 403
      );
    });

    await t.test('elimina la fotografía y el archivo optimizado', async () => {
      const creada = repositorio.estado.fotografias[0];
      await servicio.eliminarFotografia(101, creada.id_foto, 7);
      assert.equal(repositorio.estado.fotografias.length, 0);
      await assert.rejects(
        almacenamiento.obtenerArchivo(creada.ruta_relativa),
        (error) => error.codigo === 'ARCHIVO_NO_ENCONTRADO'
      );
    });

    await t.test('cambia orden y establece una portada inequívoca', async () => {
      const creadas = await servicio.subirFotografias(
        101,
        7,
        [archivo(jpeg), archivo(jpeg), archivo(jpeg)]
      );
      const portada = creadas[1].id_foto;
      const ordenadas = await servicio.reordenarFotografias(
        101,
        7,
        creadas.map((foto) => foto.id_foto),
        portada
      );

      assert.equal(ordenadas[0].id_foto, portada);
      assert.equal(ordenadas[0].es_portada, true);
      assert.equal(
        ordenadas.filter((foto) => foto.es_portada).length,
        1
      );
    });

    await t.test('rechaza intentos de salir del storage autorizado', () => {
      assert.throws(
        () => almacenamiento.resolverRutaSegura('../../fuera.webp'),
        (error) => error.codigo === 'RUTA_INSEGURA'
      );
      assert.throws(
        () => almacenamiento.resolverRutaSegura('propiedades/101/../fuera.webp'),
        (error) => error.codigo === 'RUTA_INSEGURA'
      );
    });

    await t.test('conserva propiedades antiguas sin fotografías', async () => {
      repositorio.estado.propiedades.set(202, {
        id_propiedad: 202,
        id_propietario: 7
      });
      const fotografias = await servicio.listarFotografias(202);
      assert.deepEqual(fotografias, []);
    });

    await t.test('no amplía la administración al rol administrador', () => {
      const req = { usuario: { id_usuario: 1, rol: 'administrador' } };
      const res = respuesta();
      let siguiente = false;
      exigirPropietario(req, res, () => {
        siguiente = true;
      });
      assert.equal(siguiente, false);
      assert.equal(res.statusCode, 403);
    });

    await t.test('registra las cinco rutas nuevas detrás de JWT', () => {
      const esperadas = [
        ['get', '/:id/fotos'],
        ['get', '/:id/fotos/:idFoto/archivo'],
        ['post', '/:id/fotos'],
        ['put', '/:id/fotos/orden'],
        ['delete', '/:id/fotos/:idFoto']
      ];

      for (const [metodo, ruta] of esperadas) {
        const capa = propiedadRoutes.stack.find(
          (item) => item.route?.path === ruta && item.route.methods[metodo]
        );
        assert.ok(capa, `${metodo.toUpperCase()} ${ruta} no fue registrada`);
        assert.equal(capa.route.stack[0].handle, verificarToken);
      }
    });
  } finally {
    await fs.rm(temporal, { recursive: true, force: true });
  }
});

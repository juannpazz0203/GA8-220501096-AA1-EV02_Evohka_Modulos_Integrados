const test = require('node:test');
const assert = require('node:assert/strict');
const vencimientos = require('../src/services/vencimientoService');
const notificaciones = require('../src/services/notificacionService');

function contrato(cambios = {}) {
  return {
    id_contrato: 8,
    estado: 'activo',
    fecha_inicio: '2026-01-15',
    fecha_fin: '2026-12-31',
    id_propietario: 1,
    id_arrendatario: 2,
    titulo_propiedad: 'Apartamento Centro',
    ...cambios
  };
}

test('reglas de vencimiento de contratos', async (t) => {
  await t.test('más de 7 días no genera aviso', () => {
    assert.equal(
      vencimientos.crearAvisoContrato(
        contrato({ fecha_fin: '2026-11-13' }),
        '2026-11-05'
      ),
      null
    );
  });

  await t.test('exactamente 7 días genera aviso', () => {
    const aviso = vencimientos.crearAvisoContrato(
      contrato({ fecha_fin: '2026-11-12' }),
      '2026-11-05'
    );
    assert.equal(aviso?.tipo, 'contrato_7_dias');
  });

  await t.test('a 6 días no repite el aviso de 7 días', () => {
    assert.equal(
      vencimientos.crearAvisoContrato(
        contrato({ fecha_fin: '2026-11-11' }),
        '2026-11-05'
      ),
      null
    );
  });

  await t.test('el día del vencimiento genera aviso', () => {
    assert.equal(
      vencimientos.crearAvisoContrato(
        contrato({ fecha_fin: '2026-11-05' }),
        '2026-11-05'
      )?.tipo,
      'contrato_hoy'
    );
  });

  await t.test('finalizado o cancelado no genera aviso', () => {
    assert.equal(
      vencimientos.crearAvisoContrato(
        contrato({ estado: 'finalizado', fecha_fin: '2026-11-05' }),
        '2026-11-05'
      ),
      null
    );
    assert.equal(
      vencimientos.crearAvisoContrato(
        contrato({ estado: 'cancelado', fecha_fin: '2026-11-05' }),
        '2026-11-05'
      ),
      null
    );
  });
});

test('reglas del ciclo mensual de pago', async (t) => {
  await t.test('más de 3 días no genera aviso', () => {
    assert.equal(
      vencimientos.crearAvisoPago(contrato(), '2026-11-11'),
      null
    );
  });

  await t.test('exactamente 3 días genera aviso', () => {
    const aviso = vencimientos.crearAvisoPago(contrato(), '2026-11-12');
    assert.equal(aviso?.tipo, 'pago_3_dias');
    assert.equal(aviso?.ciclo, '2026-11');
  });

  await t.test('el día del pago genera aviso', () => {
    assert.equal(
      vencimientos.crearAvisoPago(contrato(), '2026-11-15')?.tipo,
      'pago_hoy'
    );
  });

  await t.test('un contrato no activo no genera recordatorio', () => {
    assert.equal(
      vencimientos.crearAvisoPago(
        contrato({ estado: 'cancelado' }),
        '2026-11-15'
      ),
      null
    );
  });

  await t.test('día 31 usa el último día de febrero', () => {
    assert.equal(
      vencimientos.calcularVencimientoMensual(
        '2026-01-31',
        '2027-02-01'
      ),
      '2027-02-28'
    );
    assert.equal(
      vencimientos.calcularVencimientoMensual(
        '2026-01-31',
        '2028-02-01'
      ),
      '2028-02-29'
    );
  });

  await t.test('tres días antes puede caer en el mes y año anterior', () => {
    const inicioDiaUno = contrato({
      fecha_inicio: '2025-06-01',
      fecha_fin: '2027-12-31'
    });
    const avisoMes = vencimientos.crearAvisoPago(
      inicioDiaUno,
      '2026-10-29'
    );
    assert.equal(avisoMes?.fecha_objetivo, '2026-11-01');
    assert.equal(avisoMes?.ciclo, '2026-11');

    const avisoAnio = vencimientos.crearAvisoPago(
      inicioDiaUno,
      '2026-12-29'
    );
    assert.equal(avisoAnio?.fecha_objetivo, '2027-01-01');
    assert.equal(avisoAnio?.ciclo, '2027-01');
  });

  await t.test('un pago registrado en AAAA-MM satisface el ciclo', async () => {
    const ejecutor = {
      async execute(sql, parametros) {
        assert.match(sql, /DATE_FORMAT\(fecha_pago, '%Y-%m'\)/);
        assert.deepEqual(parametros, [8, '2026-11']);
        return [[{ id_pago: 9 }]];
      }
    };
    assert.equal(
      await vencimientos.cicloPagado(ejecutor, 8, '2026-11'),
      true
    );
  });
});

test('la creación determinística de recordatorios es idempotente', async () => {
  const guardadas = [];
  const ejecutor = {
    async execute(sql, parametros = []) {
      if (sql.includes('GET_LOCK')) return [[{ adquirido: 1 }]];
      if (sql.includes('RELEASE_LOCK')) return [[{ liberado: 1 }]];
      if (sql.includes('SELECT id_notificacion')) {
        const [idUsuario, titulo, mensaje] = parametros;
        return [guardadas.filter((item) =>
          item.id_usuario === idUsuario &&
          item.titulo === titulo &&
          item.mensaje === mensaje
        )];
      }
      if (sql.includes('INSERT INTO notificaciones')) {
        guardadas.push({
          id_notificacion: guardadas.length + 1,
          id_usuario: parametros[0],
          titulo: parametros[1],
          mensaje: parametros[2]
        });
        return [{ insertId: guardadas.length }];
      }
      throw new Error(`SQL no esperado: ${sql}`);
    }
  };

  assert.equal(
    await notificaciones.crearNotificacionSiNoExiste(
      ejecutor,
      2,
      'Tu pago vence hoy',
      'Contrato #8 · ciclo 2026-11'
    ),
    true
  );
  assert.equal(
    await notificaciones.crearNotificacionSiNoExiste(
      ejecutor,
      2,
      'Tu pago vence hoy',
      'Contrato #8 · ciclo 2026-11'
    ),
    false
  );
  assert.equal(guardadas.length, 1);
});

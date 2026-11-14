# Base de datos y migraciones de EVOHKA

La base oficial es `gestion_arriendos`. El archivo `gestion_arriendos.sql`
define el esquema base de ocho tablas; el esquema final requiere las
migraciones RNF03 y fotografías.

## Orden del esquema final

1. Importar `gestion_arriendos.sql` en una base vacía.
2. Aplicar `002_rnf03_contrasena_algoritmo_produccion.sql` mediante una copia
   privada autorizada.
3. Convertir las credenciales legacy con
   `backend/scripts/migrarContrasenasBcrypt.js`, únicamente si existen.
4. Aplicar `003_rnf03_jwt_recuperacion.sql` mediante una copia privada
   autorizada.
5. Aplicar `004_propiedad_fotos.sql` mediante una copia privada autorizada.
6. Verificar el esquema antes de habilitar sesiones, recuperación y
   fotografías.

El resultado esperado contiene diez tablas: las ocho del esquema base más
`recuperaciones_contrasena` y `propiedad_fotos`. `usuarios` incorpora además
`contrasena_algoritmo` y `sesion_version`.

`001_rnf03_contrasena_algoritmo.sql` está limitado a
`gestion_arriendos_respaldo_test`. No forma parte de una instalación de
producción.

## Regla de autorización

Los SQL versionados deben permanecer bloqueados en Git. Para una ejecución
autorizada:

1. Detener el backend o impedir escrituras.
2. Generar un respaldo actualizado de MySQL.
3. Restaurarlo en una base independiente nueva, sin borrar ni sobrescribir una
   base existente.
4. Verificar conteos, estructura y capacidad real de restauración.
5. Auditar el estado actual de la base objetivo mediante consultas agregadas
   que no muestren contraseñas ni hashes.
6. Crear una copia privada del SQL fuera del repositorio.
7. Completar en esa copia únicamente las autorizaciones y conteos que hayan
   sido comprobados en ese momento.
8. Seleccionar explícitamente `gestion_arriendos` e importar la copia completa
   en una sola sesión.
9. Comprobar el resultado antes de iniciar el siguiente bloque.

No elimine llamadas de aborto, comprobaciones de `DATABASE()`, validaciones de
esquema ni comparaciones `utf8mb4_bin`. La idempotencia no sustituye el respaldo
ni la auditoría previa. MySQL realiza confirmaciones implícitas alrededor de
DDL; ante un fallo, inspeccione primero el estado y no improvise una reversión.

## Migración 002 — bcrypt

El archivo versionado exige en su copia privada:

| Variable SQL | Valor autorizado esperado |
| --- | --- |
| `@rnf03_prod_confirmar_base` | `AUTORIZO_GESTION_ARRIENDOS_RNF03` |
| `@rnf03_prod_confirmar_respaldo` | `RESPALDO_RNF03_PRODUCCION_VERIFICADO` |
| `@rnf03_prod_confirmar_ddl` | `APLICAR_COLUMNA_RNF03_PRODUCCION` |
| `@rnf03_prod_confirmar_clasificacion` | `CLASIFICAR_MARCADORES_RNF03_PRODUCCION` |
| `@rnf03_prod_candidatas_marcador_esperadas` | Conteo obtenido en la auditoría inmediata. |
| `@rnf03_prod_colisiones_marcador_confirmadas` | Debe ser `0`; cualquier otro valor bloquea el proceso. |

La migración agrega `usuarios.contrasena_algoritmo` y clasifica únicamente
marcadores bcrypt que coincidan exactamente. No convierte las credenciales
legacy restantes.

El migrador Node.js requiere confirmaciones temporales de proceso diferentes a
las del SQL:

| Variable temporal | Valor de producción |
| --- | --- |
| `EVOHKA_MIGRACION_MODO` | `produccion` |
| `EVOHKA_MIGRACION_BASE_ESPERADA` | `gestion_arriendos` |
| `EVOHKA_CONFIRMAR_MIGRACION_CONTRASENAS` | `MIGRAR_BCRYPT_RNF03_PRODUCCION` |
| `EVOHKA_AUTORIZAR_BASE_PRODUCCION` | `AUTORIZO_GESTION_ARRIENDOS_RNF03` |
| `EVOHKA_CONFIRMAR_RESPALDO_PRODUCCION` | `RESPALDO_RNF03_PRODUCCION_VERIFICADO` |
| `EVOHKA_MIGRACION_LOTE` | Opcional, entre 1 y 200. |

También requiere que `DB_NAME` coincida exactamente con la base esperada. El
script consulta `SELECT DATABASE()` usando la misma conexión antes de modificar
filas y utiliza actualizaciones condicionales para evitar doble hash o cambios
concurrentes. Estas variables no deben guardarse en `.env` después de la
migración.

Antes de avanzar, verifique solo conteos: total de usuarios, algoritmos legacy,
algoritmos bcrypt, formatos bcrypt válidos, credenciales vacías y valores por
encima del límite admitido. No incluya credenciales ni hashes en el informe.

## Migración 003 — sesiones y recuperación

Requiere que la migración bcrypt esté completa y el respaldo posterior haya
sido restaurado y comprobado. Su copia privada exige:

| Variable SQL | Valor autorizado esperado |
| --- | --- |
| `@rnf03_b2_confirmar_base` | `AUTORIZO_RNF03_BLOQUE2_GESTION_ARRIENDOS` |
| `@rnf03_b2_confirmar_respaldo` | `RESPALDO_RNF03_BLOQUE2_VERIFICADO` |
| `@rnf03_b2_confirmar_ddl` | `APLICAR_ESQUEMA_JWT_RECUPERACION_RNF03` |

Agrega `usuarios.sesion_version` y crea
`recuperaciones_contrasena` con índices, InnoDB y clave foránea hacia
`usuarios`. No crea códigos ni cambia contraseñas.

Después de verificar la migración, configure
`EVOHKA_SESIONES_VERSIONADAS_ACTIVAS=true`. Habilite recuperación únicamente
cuando SMTP y pepper estén configurados de forma privada.

## Migración 004 — fotografías

El archivo permanece bloqueado con:

```text
@evohka_fotos_confirmar_ddl = PENDIENTE_AUTORIZACION
```

La copia privada solo puede sustituirlo por
`AUTORIZO_CREAR_PROPIEDAD_FOTOS` después de validar la base activa, la tabla
`propiedades` y un respaldo restaurable. La migración crea
`propiedad_fotos`, sus índices únicos y una clave foránea con
`ON DELETE CASCADE`.

La verificación final debe confirmar estructura e índices compatibles, clave
foránea correcta y conteo esperado de fotografías, sin imprimir rutas privadas.

## Respaldo conjunto de datos y fotografías

La tabla `propiedad_fotos` guarda rutas relativas; los archivos reales están en
`backend/storage/propiedades`. Un respaldo utilizable debe capturar ambos
componentes en la misma ventana:

1. Bloquear nuevas escrituras o detener el backend.
2. Generar el dump de `gestion_arriendos` solicitando la contraseña de MySQL de
   forma interactiva o mediante un archivo privado de cliente.
3. Copiar `backend/storage/propiedades` sin cambiar su estructura relativa.
4. Guardar dump y archivos fuera de Git, junto con fecha, versión del esquema y
   checksums.
5. Restaurar primero en una base y directorio independientes que no existan.
6. Comparar conteos por tabla y comprobar que cada ruta registrada tenga su
   archivo, y que no existan archivos inesperados.

Nunca coloque el dump, archivos WebP, credenciales o copias SQL autorizadas en
el repositorio. Si falla una migración, conserve el estado para diagnóstico y
restaure el conjunto verificado; no borre tablas ni bases como primer intento.

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
6. Probar `005_usuarios_correo_unico.sql` en una restauración aislada y
   aplicarla únicamente con autorización específica.
7. Verificar el esquema antes de habilitar sesiones, recuperación y
   fotografías.

El resultado esperado contiene diez tablas: las ocho del esquema base más
`recuperaciones_contrasena` y `propiedad_fotos`. `usuarios` incorpora además
`contrasena_algoritmo` y `sesion_version`.

La migración 005 no crea tablas ni columnas: añade integridad a
`usuarios.correo`. El archivo está preparado y bloqueado en el repositorio. No
debe afirmarse que la base desplegada ya contiene sus restricciones hasta
comprobar `information_schema` en esa base.

En la instancia local auditada de `gestion_arriendos`, la migración 005 fue
aplicada el 20 de noviembre de 2026 después de restaurar y comparar un respaldo
coordinado. Se verificaron `uq_usuarios_correo`,
`chk_usuarios_correo_normalizado`, la idempotencia y la conservación de los
conteos y checksums de las diez tablas. Este resultado no acredita otras
instancias.

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

## Migración 005 — integridad del correo

`005_usuarios_correo_unico.sql` está preparado para MySQL 8.4. Estar versionado
no demuestra que se haya aplicado en una instancia; la instancia local auditada
sí fue verificada como se documenta arriba. Su copia privada autorizada
requiere:

| Variable SQL | Valor autorizado esperado |
| --- | --- |
| `@evohka_correo_confirmar_base` | `AUTORIZO_GESTION_ARRIENDOS_CORREO_UNICO` |
| `@evohka_correo_confirmar_respaldo` | `RESPALDO_CORREO_UNICO_VERIFICADO` |
| `@evohka_correo_confirmar_ddl` | `APLICAR_UNICIDAD_CORREO_USUARIOS` |
| `@evohka_correo_usuarios_esperados` | Conteo total auditado inmediatamente antes del DDL. |
| `@evohka_correo_duplicados_esperados` | Debe ser `0`. |
| `@evohka_correo_no_canonicos_esperados` | Debe ser `0`. |

La migración aborta si la base, tabla o columna no coincide con el esquema
esperado; si encuentra grupos duplicados por `LOWER(TRIM(correo))`; si existen
correos con mayúsculas o espacios; o si detecta índices o restricciones con el
mismo propósito pero estructura incompatible. No corrige ni elimina registros.

El DDL crea `uq_usuarios_correo` y
`chk_usuarios_correo_normalizado`. El `CHECK` utiliza
`CAST(... AS BINARY)`, `LOWER` y `TRIM`: MySQL 8.4 permite funciones integradas
deterministas en restricciones `CHECK`, y `CAST(... AS BINARY)` evita el
operador `BINARY` obsoleto. La salida de `trim().toLowerCase()` del backend
cumple esta comprobación. El formato completo del correo continúa validándose
en Node.js.

La columna conserva la collation existente `utf8mb4_general_ci`. Por tanto, el
índice único usa sus equivalencias —incluidas comparaciones insensibles a
mayúsculas y determinadas equivalencias de caracteres—, mientras que la
normalización del backend se limita a `trim().toLowerCase()` de JavaScript. Las
reglas Unicode de ambos motores no son idénticas para todos los caracteres y la
base puede considerar duplicadas variantes que JavaScript no transforma al
mismo texto. Esta limitación se acepta para el alcance académico de EVOHKA; no
se cambia la collation ni se amplía la normalización. El backend mantiene el
HTTP 409 genérico ante una colisión detectada por MySQL.

Para probarla sin tocar la base principal:

1. Detener escrituras y obtener el respaldo conjunto descrito abajo.
2. Levantar una instancia MySQL desechable en otro puerto o equipo. Dentro de
   esa instancia aislada, crear una base nueva llamada exactamente
   `gestion_arriendos`; así se conservan intactas las guardas del archivo.
3. Restaurar allí el dump y comparar los conteos por tabla. Nunca usar la
   instancia ni el puerto de la base principal para esta prueba.
4. Crear una copia privada de la migración y completar únicamente sus tokens
   de autorización y conteos; no cambiar nombres de base, tablas o guardas.
5. Completar los tres conteos con los valores recién auditados.
6. Ejecutar la copia completa en una sola sesión MySQL.
7. Verificar `information_schema.statistics`,
   `information_schema.table_constraints` e
   `information_schema.check_constraints`.
8. Repetir la migración autorizada en la misma base aislada para comprobar la
   idempotencia.
9. Con datos sintéticos, comprobar que una segunda variante del mismo correo
   produce `ER_DUP_ENTRY` y que un correo no canónico viola el `CHECK`.
10. Descartar únicamente la base aislada, después de conservar el informe; no
    trasladar sus tokens al archivo versionado.

Estas comprobaciones con MySQL real no forman parte de `npm test` y requieren
autorización específica.

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

Un comando de referencia para el dump, ejecutado desde una consola privada y
sin incluir la contraseña en la línea de comandos, es:

```powershell
mysqldump -u <USUARIO_MYSQL> -p --single-transaction --routines --triggers --events gestion_arriendos > <RUTA_PRIVADA>\gestion_arriendos_YYYYMMDD_HHMM.sql
```

Después debe copiarse `backend/storage/propiedades` a la misma unidad de
respaldo, conservando rutas relativas, y calcular checksums de ambos
componentes. La capacidad de restauración debe comprobarse en una base y un
directorio nuevos antes de autorizar cualquier DDL.

Nunca coloque el dump, archivos WebP, credenciales o copias SQL autorizadas en
el repositorio. Si falla una migración, conserve el estado para diagnóstico y
restaure el conjunto verificado; no borre tablas ni bases como primer intento.

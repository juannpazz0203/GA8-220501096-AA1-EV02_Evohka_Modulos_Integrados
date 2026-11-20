# EVOHKA — Gestión de arriendos

EVOHKA es una aplicación académica desarrollada para el programa SENA ADSO. Su
objetivo es centralizar la publicación de inmuebles, las solicitudes de
arriendo, los contratos, los pagos y la comunicación entre propietarios y
arrendatarios desde una aplicación Android nativa.

El repositorio contiene la aplicación Android, una API REST, el esquema base de
MySQL y las migraciones versionadas necesarias para obtener el esquema actual.
No incluye credenciales, respaldos, fotografías de usuarios ni configuraciones
privadas listas para producción.

## Estado funcional

| Código | Requisito | Estado actual |
| --- | --- | --- |
| RF01 | Registro y gestión de propiedades | Implementado: crear, consultar, editar, eliminar y administrar fotografías propias. |
| RF02 | Contratos | Implementado. |
| RF03 | Pagos | Implementado. |
| RF04 | Reportes | Implementado como reporte de ingresos del propietario. |
| RF05 | Fallas | Implementado para reporte del arrendatario y gestión del propietario. |
| RF06 | Notificaciones | Implementado, incluidos lectura, contador y recordatorios. |
| RF07 | Registro, autenticación y sesión | Implementado con bcrypt, JWT y sesión Android cifrada. |
| RF08 | Consulta y detalle de propiedades | Implementado, incluida la galería de fotografías. |
| RF09 | Solicitud de arriendo | Implementado: el arrendatario solicita y el propietario responde. |
| RF10 | Comunicación | Implementado mediante conversaciones y mensajes. |
| RF11 | Administración | Implementado para gestión de usuarios y resumen administrativo. |
| RF12 | FAQ/chatbot | Parcial: existe una FAQ local con buscador. No existe un chatbot funcional. |

RNF03 está implementado mediante contraseñas bcrypt, JWT con sesiones
versionadas, recuperación de contraseña por código temporal y almacenamiento
cifrado del token en Android.

## Tecnologías

- Android nativo con Kotlin y vistas XML.
- Retrofit, Gson y OkHttp para consumir la API.
- Coil para cargar fotografías autenticadas.
- Node.js 20.9 o posterior, Express y MySQL2.
- MySQL 8.4, con tablas InnoDB y migraciones protegidas.
- JWT HS256 con `issuer`, `audience`, expiración y versión de sesión.
- bcryptjs para las contraseñas.
- Nodemailer y SMTP para recuperación de contraseña.
- Multer y Sharp para recepción y conversión de imágenes a WebP.

## Arquitectura

```text
Android Kotlin/XML
    │
    │ Retrofit / OkHttp / JWT
    ▼
API Node.js + Express
    ├── MySQL gestion_arriendos
    ├── backend/storage/propiedades (archivos WebP)
    └── Servidor SMTP (recuperación de contraseña)
```

MySQL almacena los datos funcionales y las rutas relativas de las fotografías.
Los archivos optimizados se guardan fuera de la base de datos en
`backend/storage/propiedades/{id_propiedad}/`. Esa carpeta está excluida de Git
y debe respaldarse junto con MySQL.

## Estructura del repositorio

```text
android/EvohkaApp/       Aplicación Android y Gradle Wrapper
backend/                 API Node.js/Express
  src/                   Rutas, controladores, servicios y configuración
  test/                  Pruebas automatizadas con dependencias simuladas
  storage/               Fotografías locales; ignorado por Git
database/
  gestion_arriendos.sql  Esquema base de ocho tablas
  migrations/            Migraciones protegidas RNF03 y fotografías
AGENTS.md                 Reglas permanentes de trabajo
README.md                 Guía principal
```

## Requisitos de instalación

- Git.
- Node.js 20.9 o posterior y npm. Las dependencias nativas actuales, como
  Sharp, requieren esa versión mínima.
- MySQL 8.4 o una versión compatible que soporte las expresiones utilizadas en
  las migraciones.
- JDK 17 para ejecutar Gradle. El código Android se compila con compatibilidad
  Java 11.
- Android SDK correspondiente a `compileSdk 36.1` y `targetSdk 36`.
- Android 6.0/API 23 o posterior en el dispositivo.
- Para una tablet física: computador y tablet en la misma red, backend
  escuchando en el puerto configurado y firewall permitiendo ese puerto.

## Configurar la base de datos

El archivo `database/gestion_arriendos.sql` es el esquema base y crea ocho
tablas. No representa por sí solo el esquema final de diez tablas.

El orden de preparación es:

1. Crear una base vacía llamada `gestion_arriendos`.
2. Importar `database/gestion_arriendos.sql`.
3. Auditar y aplicar una copia privada autorizada de
   `002_rnf03_contrasena_algoritmo_produccion.sql`.
4. Si existen credenciales legacy, convertirlas con el migrador Node.js
   protegido antes de habilitar el backend normal.
5. Auditar y aplicar una copia privada autorizada de
   `003_rnf03_jwt_recuperacion.sql`.
6. Auditar y aplicar una copia privada autorizada de
   `004_propiedad_fotos.sql`.
7. Probar en una restauración aislada y, solo con autorización específica,
   aplicar una copia privada de `005_usuarios_correo_unico.sql`.
8. Verificar estructura, índices, claves foráneas y conteos antes de iniciar la
   API.

`001_rnf03_contrasena_algoritmo.sql` es exclusiva para la base de validación
`gestion_arriendos_respaldo_test`; no debe ejecutarse sobre producción.

Las migraciones 002–005 están bloqueadas de forma intencional. Validan la base
activa, el esquema esperado y confirmaciones explícitas antes de cualquier
DDL/DML. No se deben eliminar sus guardas ni versionar copias con las
autorizaciones habilitadas. Consulte [database/README.md](database/README.md)
para el procedimiento detallado.

La migración 005 permanece preparada y bloqueada en el repositorio. En la
instancia local auditada de `gestion_arriendos` fue aplicada y verificada el
20 de noviembre de 2026; cualquier otra instancia debe comprobarse de forma
independiente en `information_schema`. El backend conserva compatibilidad antes
y después de la migración, pero la garantía definitiva frente a escrituras
concurrentes depende de que el índice único exista en la base utilizada.

## Configurar el backend

1. Entrar en `backend`.
2. Instalar exactamente el árbol bloqueado:

   ```powershell
   npm ci
   ```

3. Copiar `.env.example` como `.env`.
4. Completar localmente los valores privados. `.env` está ignorado por Git.
5. Confirmar que las migraciones requeridas ya fueron aplicadas.
6. Iniciar la API sin configuración privada adicional:

   ```powershell
   npm start
   ```

En el equipo local de EVOHKA, el procedimiento oficial para iniciar RNF03 con
sesiones versionadas y recuperación habilitadas es ejecutar el iniciador
privado `Iniciar-EvohkaRecuperacion.ps1`, conservado fuera del repositorio. El
iniciador carga la configuración privada únicamente en el proceso hijo y luego
ejecuta el mismo arranque de npm. No copie al repositorio el iniciador, su
almacén protegido, rutas privadas, credenciales ni valores de configuración.
Ejecutar `npm start` directamente utiliza solo el entorno y `.env` disponibles
en esa consola.

El arranque valida la configuración JWT, `usuarios.contrasena_algoritmo` y,
cuando las sesiones versionadas están activas, las estructuras de la migración
003. Si el esquema no coincide, el backend se detiene en lugar de degradarse a
un modo inseguro.

### Variables principales

| Variable | Uso |
| --- | --- |
| `PORT` | Puerto HTTP de la API; normalmente 3000. |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` | Conexión MySQL. |
| `JWT_SECRET` | Secreto aleatorio privado de al menos 32 bytes UTF-8. |
| `EVOHKA_SESIONES_VERSIONADAS_ACTIVAS` | Debe ser `true` con el esquema final. |
| `EVOHKA_BCRYPT_COSTO` | Coste opcional entre 10 y 14; el valor normal es 12. |
| `EVOHKA_RECUPERACION_ACTIVA` | Habilita la recuperación solo cuando SMTP y pepper estén listos. |
| `EVOHKA_RECUPERACION_PEPPER` | Pepper privado de al menos 32 bytes UTF-8. |
| `EVOHKA_SMTP_*` | Host, puerto, seguridad, usuario, contraseña de aplicación y remitente. |

Las variables de autorización del migrador de contraseñas son de un solo uso.
No deben guardarse permanentemente en `.env`; se inyectan únicamente durante
una migración expresamente autorizada.

### Crear usuarios de prueba

No hay credenciales de prueba compartidas en este repositorio. Cree cuentas
ficticias propias mediante la pantalla de registro o `POST /api/auth/registro`.
Los campos obligatorios son `nombre`, `apellido`, `correo`, `contrasena` y
`rol`; `telefono` es opcional. Utilice un correo reservado para pruebas, una
contraseña privada que no se reutilice y elimine las cuentas temporales al
finalizar.

## Configurar Android

1. Copiar `android/EvohkaApp/evohka.properties.example` como
   `android/EvohkaApp/evohka.properties`.
2. Configurar las dos URL base. El archivo real está ignorado por Git.
3. Desde `android/EvohkaApp`, compilar:

   ```powershell
   .\gradlew.bat testDebugUnitTest
   .\gradlew.bat assembleDebug
   ```

El APK debug se genera en:

```text
android/EvohkaApp/app/build/outputs/apk/debug/app-debug.apk
```

### Red local

- Emulador Android estándar: `http://10.0.2.2:3000/` representa el computador
  anfitrión.
- Tablet física: utilice la IPv4 actual del computador, por ejemplo
  `http://192.168.x.x:3000/`. La dirección puede cambiar al reconectar la red.
- La URL debe terminar en `/`.
- El backend debe estar accesible desde la red y el firewall debe permitir el
  puerto. Cambiar la URL no corrige por sí solo un bloqueo de red.
- En un despliegue futuro, configure ambas URL con un dominio HTTPS, por
  ejemplo `https://api.ejemplo.com/`, y retire el tráfico HTTP claro de la
  configuración Android.

`BackendUrlProvider` selecciona automáticamente la URL de emulador o de
dispositivo físico. La misma base se utiliza para autenticación, RF01–RF12 y
fotografías.

## Recuperación de contraseña

La recuperación utiliza un código de ocho dígitos con vigencia de 15 minutos y
máximo cinco intentos fallidos. En MySQL solo se almacena un HMAC del código,
su salt y las fechas de control. Un restablecimiento exitoso invalida el código
y revoca sesiones anteriores.

Para habilitarla se requieren las estructuras de la migración 003, sesiones
versionadas activas, un pepper privado y una cuenta SMTP. Las credenciales SMTP
no deben colocarse en comandos compartidos, documentación, Git ni registros.
Para Gmail se debe usar una contraseña de aplicación, nunca la contraseña
principal de la cuenta.

## Fotografías

- Máximo 10 fotografías por inmueble.
- Máximo 15 MB por archivo original.
- Formatos admitidos: JPG, PNG y WebP.
- Conversión automática a WebP, lado mayor máximo de 1920 píxeles y calidad
  85.
- Una fotografía puede ocupar el orden de portada.
- Los endpoints de lectura y administración están protegidos con JWT.
- Solo el propietario autenticado puede modificar las fotografías de su
  inmueble.

## Pruebas

Backend:

```powershell
cd backend
npm test
```

Android:

```powershell
cd android/EvohkaApp
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Último estado automatizado validado el 20 de noviembre de 2026:

- Backend: 86 pruebas aprobadas, 0 fallidas y 0 omitidas.
- Android: 45 pruebas aprobadas, 0 fallidas y 0 omitidas.
- Total: 131 pruebas aprobadas; `assembleDebug` completado correctamente.

Una entrega nueva debe guardar un informe fechado de su propia ejecución
consolidada.

## Respaldo y restauración

Un respaldo completo requiere dos componentes tomados en la misma ventana de
mantenimiento:

1. Dump de `gestion_arriendos` con estructura y datos.
2. Copia íntegra de `backend/storage/propiedades`.

Detenga o bloquee escrituras durante la copia, almacene ambos elementos fuera
del repositorio, registre fecha y checksums, y pruebe la restauración en una
base y un directorio independientes. Restaurar solo MySQL conserva las rutas,
pero pierde los archivos; restaurar solo `storage` deja archivos sin metadatos.

## Limitaciones y requisitos para producción

- RF12 ofrece FAQ local con buscador; no hay chatbot funcional.
- La configuración actual está orientada a una red local de demostración.
- El tráfico debe migrarse a HTTPS antes de un despliegue real.
- Las variables privadas requieren un almacén de secretos del servidor.
- `backend/storage` requiere volumen persistente y respaldo coordinado.
- Los límites globales/IP de recuperación viven en memoria y no se comparten
  entre múltiples instancias.
- `usuarios.correo` conserva `utf8mb4_general_ci`. Su índice único aplica las
  equivalencias de esa collation, que no son idénticas a todas las reglas
  Unicode de `toLowerCase()` de JavaScript; esta limitación se acepta para el
  alcance académico actual y no implica cambiar la collation.
- Deben revisarse dependencias y vulnerabilidades antes de cada entrega, sin
  aplicar actualizaciones forzadas sin revisión.
- Deben definirse dominio, certificados, servidor, monitoreo, rotación de
  credenciales y política de recuperación ante desastres.
- El APK debug no sustituye una versión release firmada para distribución.

## Seguridad documental

- No versionar `.env`, `evohka.properties`, respaldos, APK, fotografías ni
  claves de firma.
- No documentar contraseñas, JWT, peppers, hashes ni contraseñas de aplicación.
- Los archivos `.example` contienen únicamente nombres, valores públicos y
  marcadores vacíos.
- Las cuentas temporales deben ser identificables y eliminarse al terminar las
  pruebas.

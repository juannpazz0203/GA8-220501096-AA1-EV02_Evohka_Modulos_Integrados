# Backend de EVOHKA

API REST de EVOHKA desarrollada con Node.js, Express y MySQL. Expone los
módulos de autenticación, propiedades, fotografías, solicitudes, contratos,
pagos, reportes, notificaciones, fallas, mensajes y administración.

La guía integral de instalación se encuentra en el [README principal](../README.md).

## Requisitos

- Node.js 20.9 o posterior.
- MySQL con el esquema base y las migraciones 002, 003 y 004 aplicadas.
- Un archivo `.env` privado creado a partir de `.env.example`.

## Instalación y ejecución

```powershell
npm ci
npm test
npm start
```

El servidor valida antes de escuchar el puerto:

- conexión a MySQL;
- configuración y longitud mínima de `JWT_SECRET`;
- columna `usuarios.contrasena_algoritmo`;
- `usuarios.sesion_version` y `recuperaciones_contrasena` cuando las sesiones
  versionadas están activas.

La aplicación debe iniciarse con `EVOHKA_SESIONES_VERSIONADAS_ACTIVAS=true`
después de aplicar la migración 003. La recuperación permanece desactivada
hasta configurar SMTP, pepper y `EVOHKA_RECUPERACION_ACTIVA=true`.

## Crear una cuenta de prueba propia

Este repositorio no distribuye correos ni contraseñas de prueba. Se puede crear
una cuenta ficticia mediante Android o con `POST /api/auth/registro`:

```json
{
  "nombre": "<NOMBRE_FICTICIO>",
  "apellido": "<APELLIDO_FICTICIO>",
  "correo": "<CORREO_RESERVADO_PARA_PRUEBAS>",
  "contrasena": "<CONTRASENA_PRIVADA_DE_PRUEBA>",
  "telefono": "<TELEFONO_FICTICIO_OPCIONAL>",
  "rol": "arrendatario"
}
```

Son obligatorios `nombre`, `apellido`, `correo`, `contrasena` y `rol`. El rol
público admite `arrendatario`, `inquilino` —normalizado a `arrendatario`— o
`propietario`; no permite crear administradores. No reutilice contraseñas
personales y elimine las cuentas temporales al finalizar.

## Recuperación RNF03

Endpoints:

- `POST /api/auth/recuperacion/solicitar` con `correo`.
- `POST /api/auth/recuperacion/restablecer` con `correo`, `codigo` y
  `nueva_contrasena`.
- `GET /api/auth/sesion` para validar la sesión e identidad actuales.

Variables privadas requeridas al habilitar recuperación:

- `EVOHKA_RECUPERACION_ACTIVA`
- `EVOHKA_RECUPERACION_PEPPER`
- `EVOHKA_SMTP_HOST`
- `EVOHKA_SMTP_PORT`
- `EVOHKA_SMTP_SECURE`
- `EVOHKA_SMTP_USUARIO`
- `EVOHKA_SMTP_PASSWORD_APLICACION`
- `EVOHKA_SMTP_REMITENTE`

No registre códigos, contraseñas, JWT, hashes, peppers ni respuestas completas
del transporte SMTP. Para Gmail utilice una contraseña de aplicación, nunca la
contraseña principal.

## Fotografías

Los originales multipart se validan, se convierten con Sharp a WebP y se
guardan en `storage/propiedades/{id_propiedad}/`. MySQL conserva únicamente la
ruta relativa y los metadatos. `storage/` está ignorado por Git y requiere un
respaldo propio coordinado con el dump de MySQL.

## Pruebas

```powershell
npm test
```

Las pruebas utilizan dobles o repositorios simulados; no deben apuntar a la
base real. El último resultado consolidado informado fue 73 pruebas aprobadas.

## Seguridad operativa

- No versionar `.env`, secretos SMTP, respaldos ni fotografías.
- No guardar autorizaciones de migración en `.env`.
- No ejecutar migraciones sobre una base no verificada.
- No usar `npm audit fix --force`.
- Para producción, usar HTTPS, almacenamiento persistente, un gestor de
  secretos, respaldos comprobados y revisión controlada de dependencias.

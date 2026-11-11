# GA8-220501096-AA1-EV01 – Módulo de autenticación

## Descripción

Este proyecto implementa un módulo de autenticación para el sistema Evohka utilizando Node.js, Express, MySQL y JWT.

Permite:

- Inicio de sesión.
- Validación de credenciales.
- Generación de Token JWT.
- Protección de rutas mediante middleware.

## Tecnologías

- Node.js
- Express
- MySQL
- JSON Web Token
- Postman

## Instalación

1. Ejecutar:

```bash
npm install
```

2. Configurar el archivo `.env`.

3. Iniciar MySQL (Laragon o XAMPP).

4. Verificar que la base configurada ya incluya el esquema RNF03, especialmente
   `usuarios.contrasena_algoritmo`.

5. Iniciar normalmente el backend:

```bash
npm start
```

No se requiere `EVOHKA_COLUMNA_CONTRASENA_ALGORITMO_ACTIVA`. El backend valida
el esquema RNF03 antes de abrir el puerto y detiene el arranque con un error
claro si la columna obligatoria falta o es incompatible.

Para desarrollo con reinicio automático puede utilizarse `npm run dev`.

## Usuario de prueba

Correo:

```
juan@gmail.com
```

Contraseña:

```
123456
```

## Pruebas realizadas

- Login correcto.
- Validación de campos obligatorios.
- Credenciales incorrectas.
- Ruta protegida sin token.
- Ruta protegida con token válido.
- Ruta protegida con token inválido.

## Recuperación de contraseña RNF03

El backend expone los siguientes endpoints, sin modificar el contrato de
login, registro o sesión:

- `POST /api/auth/recuperacion/solicitar`, con `correo`.
- `POST /api/auth/recuperacion/restablecer`, con `correo`, `codigo` y
  `nueva_contrasena`.

La recuperación está desactivada de forma predeterminada. Para habilitarla,
el proceso del backend debe recibir de un almacén privado las variables:

- `EVOHKA_RECUPERACION_ACTIVA`
- `EVOHKA_RECUPERACION_PEPPER`
- `EVOHKA_SMTP_HOST`
- `EVOHKA_SMTP_PORT`
- `EVOHKA_SMTP_SECURE`
- `EVOHKA_SMTP_USUARIO`
- `EVOHKA_SMTP_PASSWORD_APLICACION`
- `EVOHKA_SMTP_REMITENTE`

También deben permanecer activas las sesiones versionadas mediante
`EVOHKA_SESIONES_VERSIONADAS_ACTIVAS`. No se deben versionar los valores ni
escribirlos en documentación, comandos compartidos o registros.

Antes del único envío real controlado:

1. Confirmar que la migración SQL 003 está aplicada y que la suite completa
   termina correctamente.
2. Inyectar las variables privadas únicamente en el proceso que se utilizará
   para la validación. Para Gmail se utiliza su servidor SMTP y una contraseña
   de aplicación, nunca la contraseña normal de la cuenta.
3. Iniciar una sola instancia del backend y realizar una única solicitud con
   una cuenta de prueba controlada existente.
4. Confirmar que llega un solo correo, que el código permite un único
   restablecimiento y que la sesión anterior queda revocada.
5. Retirar las variables temporales al terminar. No registrar el código, la
   contraseña, el pepper ni la respuesta del transporte SMTP.

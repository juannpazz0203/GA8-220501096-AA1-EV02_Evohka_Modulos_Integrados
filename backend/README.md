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

4. Ejecutar:

```bash
npm run dev
```

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
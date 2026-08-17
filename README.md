# GA8-220501096-AA1-EV02 - Módulos Integrados Evohka

Este repositorio contiene la integración de módulos del proyecto Evohka.

## Estructura

- `android/EvohkaApp`: aplicación Android desarrollada en Kotlin.
- `backend`: API desarrollada con Node.js, Express y MySQL.

## Integración implementada

La aplicación Android realiza el inicio de sesión mediante una petición al backend.

Flujo:

Android → Retrofit → API Node.js → MySQL → JWT → respuesta a Android

## Módulos funcionales

- Autenticación de usuarios.
- Generación de token JWT.
- Middleware de autenticación.
- Consulta de propiedades.
- Integración Android - Backend - Base de datos.

## Control de versiones

El proyecto está versionado con Git y publicado en GitHub.
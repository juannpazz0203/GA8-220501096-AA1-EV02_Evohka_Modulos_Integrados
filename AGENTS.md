# Guía permanente de trabajo para EVOHKA

Este archivo aplica a todo el repositorio. Su propósito es conservar el contexto técnico, funcional y de seguridad que debe respetarse antes de realizar cualquier cambio.

## Proyecto

- Nombre: **EVOHKA**.
- Aplicación para gestión de arriendos.
- Proyecto académico SENA ADSO.
- Plataforma final: **Android únicamente**. No desarrollar ni agregar soporte para iOS.

## Tecnologías oficiales

- Aplicación Android nativa con Kotlin y vistas XML.
- Backend con Node.js y Express.
- Base de datos MySQL.
- Comunicación Android/API con Retrofit y Gson.
- Autenticación mediante JWT.
- Control de versiones con Git y GitHub.

## Estructura actual del repositorio

- `android/EvohkaApp/`: aplicación Android. El código principal está en `app/src/main/java/com/example/evohkaapp/` y las vistas y recursos en `app/src/main/res/`.
- `backend/`: API de Node.js/Express. La entrada es `index.js`; las rutas, controladores, servicios, middleware y configuración están organizados en `src/`.
- `database/gestion_arriendos.sql`: esquema MySQL del proyecto.
- `README.md`: descripción general de la integración Android → Retrofit → API → MySQL → JWT.

## Base de datos

- La base de datos oficial es `gestion_arriendos`.
- La tabla oficial de propiedades es `propiedades`.
- No crear, recuperar ni volver a utilizar `propiedades_servicio`.
- No inventar tablas, relaciones o columnas. Comprobar primero el esquema vigente y el código que lo consume.

La estructura oficial actual de `propiedades` es:

- `id_propiedad`
- `titulo`
- `tipo_inmueble`
- `direccion`
- `barrio`
- `ciudad`
- `canon`
- `habitaciones`
- `banos`
- `id_propietario`

## Reglas de seguridad y trabajo

- Nunca modificar, borrar, mostrar, copiar a documentación ni versionar `.env`.
- Nunca modificar ni eliminar `android/EvohkaApp/evohka.properties` salvo instrucción expresa del usuario.
- `.env` contiene la configuración local del backend y está ignorado por Git.
- `android/EvohkaApp/evohka.properties` contiene la configuración Android local y está ignorado por Git.
- No borrar `.env`, `evohka.properties` ni `node_modules` por considerarlos temporales.
- No ejecutar `npm audit fix --force`.
- No borrar ni alterar destructivamente datos reales de MySQL.
- Los usuarios y datos temporales de prueba deben estar identificados claramente y eliminarse al terminar la prueba.
- No ejecutar `git commit`, `git merge`, `git push`, cambiar de rama ni borrar ramas sin autorización expresa del usuario.
- Antes de modificar código, inspeccionar los archivos relacionados y entender el flujo existente de extremo a extremo.
- Evitar reescribir funcionalidades que ya funcionan; preferir cambios pequeños, compatibles y controlables.
- Después de cambios importantes, ejecutar las compilaciones, pruebas o comprobaciones correspondientes.
- No inventar tablas, endpoints, campos, respuestas de API ni requisitos. Comprobar primero la implementación y el esquema existentes.
- Mantener compatibilidad con la base de datos y la API actuales.
- No exponer secretos, tokens JWT, credenciales, datos personales ni valores de configuración en salidas, pruebas, capturas o documentación.

## Requisitos funcionales permanentes

Mantener permanentemente la numeración **RF01–RF12**; no renumerar, reutilizar ni intercambiar estos identificadores.

- **RF01:** registro y gestión de propiedades. El registro de propiedades ya funciona. Próxima ampliación: editar y eliminar propiedades propias.
- **RF02:** contratos.
- **RF03:** pagos.
- **RF04:** reportes.
- **RF05:** fallas.
- **RF06:** notificaciones.
- **RF07:** registro, autenticación y sesión. Ya funcionan el registro de inquilino/arrendatario y propietario, el inicio de sesión, JWT y el cierre de sesión.
- **RF08:** consulta y detalle de propiedades. Ya funciona.
- **RF09:** solicitud de arriendo. Pendiente de completar.
- **RF10:** comunicación. Pendiente.
- **RF11:** administración. Pendiente.
- **RF12:** FAQ/chatbot. Existe de forma separada y aún no está integrado.

## Roles y normalización

- En la interfaz Android, el usuario puede visualizar el rol como `inquilino`.
- En la base de datos y en la API, el rol correspondiente se almacena como `arrendatario`.
- El otro rol admitido es `propietario`.
- No romper ni eliminar esta normalización entre `inquilino` y `arrendatario`.

## Diseño visual oficial

- La referencia visual oficial de EVOHKA es el proyecto de Figma [EVOHKA](https://www.figma.com/design/vTjZzt42Ly2QXTJdBXKbuu/EVOHKA).
- No inventar una nueva identidad visual si existe una pantalla equivalente en Figma.
- Antes de realizar cambios importantes de interfaz, identificar la pantalla correspondiente de Figma o solicitar al usuario/ChatGPT la referencia del nodo.
- Adaptar Figma a Android Kotlin/XML; no copiar código React, Tailwind, iOS ni componentes incompatibles con Android.
- Mantener la funcionalidad y el modelo de datos reales aunque una maqueta antigua de Figma muestre campos que ya no forman parte del sistema.
- Reutilizar los recursos visuales existentes de EVOHKA cuando corresponda.
- Los colores y el estilo utilizados actualmente incluyen el morado principal `#CE74FF`, fondos blancos y campos `#F5F5F5`.

Referencias conocidas de Figma:

- Inicio de sesión propietario: nodo `419:1990`.
- Registro de usuario: nodo `101:331`.
- Inicio inquilino / propiedades disponibles: nodo `101:437`.
- Inicio propietario: nodo `298:1018`.
- Mis inmuebles: nodo `298:1063`.
- Publicar inmueble: nodo `298:1108`.
- Solicitudes: nodo `298:1153`.
- Perfil: nodo `298:1198`.

Si el diseño de Figma contradice la base de datos o los requisitos actualmente aprobados, prevalece el modelo funcional aprobado. Figma se utiliza como referencia visual, no para reintroducir campos obsoletos.

## Reglas de sesión y navegación

- Pulsar el botón Atrás de Android nunca debe cerrar la sesión del usuario por sí solo.
- Después de iniciar sesión correctamente, la pantalla de login no debe permanecer como destino al que se regresa mediante Atrás.
- Si el usuario está en la pantalla principal autenticada y pulsa Atrás, puede salir de la aplicación, pero la sesión almacenada debe conservarse.
- Al volver a abrir EVOHKA, si existe una sesión válida, la aplicación debe restaurar el flujo correspondiente al rol autenticado sin solicitar nuevamente las credenciales.
- La sesión solamente debe eliminarse mediante la acción explícita `Cerrar sesión` o cuando sea técnicamente inválida o haya expirado.
- `Cerrar sesión` sí debe limpiar los datos locales de sesión y regresar al login, eliminando de la pila las pantallas autenticadas.
- No implementar una solución basada únicamente en bloquear `onBackPressed`. Corregir apropiadamente la navegación, el back stack y el manejo de sesión.
- Mantener la normalización de roles: `inquilino` en la interfaz corresponde a `arrendatario` en el backend y la base de datos.

## Propiedades y autorización

- Un propietario debe trabajar únicamente sobre sus propias propiedades.
- Toda edición o eliminación futura debe comprobar en el backend que `id_propietario` corresponde al usuario autenticado mediante JWT.
- Obtener la identidad del propietario desde el JWT validado; no confiar en un `id_propietario` enviado por el cliente.
- No confiar únicamente en controles de la interfaz Android para aplicar seguridad o autorización.
- Las rutas actuales de propiedades están protegidas con middleware JWT. Conservar esa protección en las ampliaciones.

## Estado actual conocido

- **DT-01**, unificación del modelo de propiedades, está completado.
- **RF07** fue implementado y probado.
- El registro y el inicio de sesión usan el estilo visual de EVOHKA basado en Figma.
- La pantalla de propiedades disponibles también fue actualizada visualmente.
- Existen drawables, layouts, colores, temas y otros recursos visuales de EVOHKA que deben revisarse y reutilizarse cuando sea apropiado.
- No reemplazar el diseño existente sin revisar primero los recursos visuales y las pantallas relacionadas.
- El backend expone actualmente autenticación en `/api/auth/login` y `/api/auth/registro`, y consulta/registro de propiedades en `/api/propiedades`.
- Android consume esos servicios mediante Retrofit y Gson, usando la URL base configurada localmente.
- La edición y eliminación de propiedades aún no están implementadas; no asumir endpoints o contratos para ellas sin inspeccionar y acordar el cambio.

## Deuda técnica conocida

- El sistema actual todavía compara y guarda contraseñas en texto plano.
- No introducir bcrypt, hashing u otro esquema de contraseñas de forma aislada.
- Existen usuarios con contraseñas en texto plano; una migración parcial o incompatible rompería el inicio de sesión.
- La migración de contraseñas debe tratarse posteriormente como una tarea de seguridad planificada, integral y compatible con los datos existentes.

## Forma obligatoria de trabajo

1. Inspeccionar el estado de Git, la estructura y los archivos relacionados antes de modificar.
2. Explicar brevemente qué se va a cambiar.
3. Realizar cambios pequeños, controlables y limitados al alcance solicitado.
4. Ejecutar el build, las pruebas o las comprobaciones pertinentes según las capas afectadas.
5. Revisar los resultados y mostrar claramente qué archivos fueron modificados.
6. Nunca hacer commit, merge o push sin autorización expresa.

Para cambios que atraviesen varias capas, comprobar el flujo completo: XML/Activity o modelo Android → Retrofit/Gson → ruta/middleware/controlador/servicio de Express → esquema MySQL. Si el alcance no exige modificar una capa, conservar su contrato actual.

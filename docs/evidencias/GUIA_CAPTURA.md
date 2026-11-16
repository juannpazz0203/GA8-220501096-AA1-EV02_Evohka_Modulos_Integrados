# Guía para obtener las evidencias

Esta guía prepara una sola ronda final. No ejecutar sus comandos ni modificar
datos hasta contar con la autorización correspondiente y el entorno de prueba
controlado.

## 1. Preparar la ronda

1. Definir una propiedad principal, un propietario, un arrendatario y, si es
   necesario, un usuario administrativo temporal, todos ficticios.
2. Usar la propiedad principal para encadenar fotografías, solicitud,
   contrato, pago, falla, mensajes y notificaciones. Preparar un segundo
   inmueble desechable, sin relaciones, únicamente para evidenciar la
   eliminación sin afectar el recorrido principal.
3. Anotar los archivos de evidencia previstos, no contraseñas ni identificadores
   sensibles.
4. Comprobar que el backend, MySQL, SMTP y red corresponden al entorno
   autorizado antes de iniciar la captura.
5. Guardar originales sin redactar fuera del repositorio y trasladar a
   `docs/evidencias` únicamente versiones saneadas.

## 2. Recorrido único en tablet

Seguir este orden para reutilizar el mismo conjunto de datos:

1. Registrar las cuentas ficticias y capturar solo la confirmación.
2. Iniciar como propietario y capturar el panel.
3. Publicar una propiedad con fotografías y capturar selección, resultado y
   portada.
4. Editar la propiedad y comprobar el regreso a Mis inmuebles.
5. Crear y eliminar el segundo inmueble desechable antes de asociarle
   solicitudes u otros registros.
6. Cerrar sesión explícitamente.
7. Iniciar como arrendatario, capturar catálogo, detalle y galería, y enviar la
   solicitud.
8. Iniciar como propietario, responder la solicitud y crear el contrato.
9. Comprobar el contrato desde ambos roles y registrar el pago previsto.
10. Reportar una falla como arrendatario y gestionarla como propietario.
11. Enviar un único mensaje ficticio y verificar la notificación relacionada.
12. Capturar el reporte de ingresos del propietario.
13. Capturar el panel administrativo y gestionar únicamente una cuenta
    temporal autorizada.
14. Capturar la FAQ buscando una palabra y desplegando su respuesta. La
    evidencia debe llamarla FAQ, no chatbot.
15. Comprobar restauración de sesión cerrando y abriendo la aplicación sin usar
    “Cerrar sesión”; después probar el logout explícito.
16. Realizar la recuperación de contraseña al final, porque revoca sesiones y
    modifica la credencial de la cuenta controlada.

Cuando una acción tenga estados antes/después, usar dos capturas o un video de
menos de un minuto. No repetir todo el flujo para obtener una segunda toma: si
la captura no expone datos y demuestra el resultado, es suficiente.

## 3. Capturas de backend y API

Usar PowerShell o Postman con un entorno local controlado:

- Estado: capturar la respuesta de la ruta pública y el código HTTP.
- Autorización: realizar solo un caso 401 sin token y un caso 403 con un rol que
  no tenga permiso.
- Rutas protegidas exitosas: la evidencia Android ya demuestra la mayoría; no
  repetir cada endpoint en Postman.

Antes de capturar en Postman:

1. Ocultar la pestaña de variables y la cabecera `Authorization`.
2. Ocultar correo, teléfono, token y datos personales del cuerpo.
3. Mostrar únicamente método, ruta, código HTTP y mensaje funcional.
4. No exportar un entorno que contenga secretos.

## 4. Evidencias de MySQL

Ejecutar únicamente consultas de lectura. Capturar resultados agregados de:

- número de tablas de `gestion_arriendos`;
- nombres de las diez tablas;
- presencia y tipo de `contrasena_algoritmo` y `sesion_version`;
- motor, columnas, índices y claves foráneas de
  `recuperaciones_contrasena` y `propiedad_fotos`;
- conteos agrupados por `contrasena_algoritmo`;
- cantidad de registros de recuperación activos/usados/revocados;
- cantidad de fotografías por propiedad, usando identificadores o títulos
  ficticios cuando sea necesario.

No ejecutar `SELECT * FROM usuarios` ni incluir `contrasena`, `codigo_hash`,
`codigo_salt` o datos personales. No ejecutar `INSERT`, `UPDATE`, `DELETE`,
`ALTER`, migraciones ni restauraciones durante la captura de inventario.

La evidencia del respaldo requiere un procedimiento independiente y
autorizado: dump de MySQL y copia de `backend/storage/propiedades`, restaurados
en destinos nuevos. El informe debe contener solo conteos, fecha y checksums;
no el dump ni las fotografías.

## 5. Pruebas y compilación

Ejecutar una sola ronda después de terminar los datos de demostración:

```powershell
cd backend
npm test
```

Capturar el comando y el resumen final, sin repetir la salida completa.

```powershell
cd android/EvohkaApp
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Capturar los resúmenes finales. Los resultados esperados históricos son 73
pruebas backend y 35 Android, pero la evidencia debe reflejar lo que realmente
termine en esa ejecución; no editar el resultado para hacerlo coincidir.

Ejecutar también desde la raíz:

```powershell
git diff --check
```

## 6. APK

Después de un `BUILD SUCCESSFUL`, verificar que exista:

```text
android/EvohkaApp/app/build/outputs/apk/debug/app-debug.apk
```

Registrar fuera de Git:

```powershell
Get-Item .\android\EvohkaApp\app\build\outputs\apk\debug\app-debug.apk |
    Select-Object Name, Length, LastWriteTime

Get-FileHash .\android\EvohkaApp\app\build\outputs\apk\debug\app-debug.apk -Algorithm SHA256
```

El TXT de evidencia puede contener nombre, tamaño, fecha y hash. No copiar el
APK a `docs/evidencias` ni agregarlo a Git.

## 7. Git y GitHub

Al finalizar y después de la autorización de integración, capturar:

```powershell
git branch --show-current
git log -1 --oneline
git status --short --branch
```

La evidencia final debe mostrar `main` sincronizada y un árbol limpio. En
GitHub, capturar el commit final, README y carpeta `docs/evidencias`; ocultar
datos privados del navegador.

## 8. Evidencias que requieren una prueba adicional

Requieren una ejecución controlada porque no existe todavía un archivo fechado:

- recorrido manual de propietario, arrendatario y administrador;
- recuperación real de una cuenta controlada;
- persistencia, logout e invalidación de sesión;
- respuestas 401 y 403 saneadas;
- inventario de MySQL y estado bcrypt agregado;
- respaldo y restauración conjunta en destinos independientes;
- suites backend y Android;
- compilación y hash del APK;
- estado final de GitHub.

No requieren repetir una prueba funcional:

- inspección de la configuración JWT;
- código Android Keystore/AES-GCM;
- documentación de instalación;
- comprobación de que las migraciones versionadas conservan sus guardas.

## 9. Cierre

1. Redactar las capturas finales.
2. Mover únicamente las versiones saneadas a su carpeta.
3. Renombrarlas según la matriz.
4. Marcar el checklist y actualizar cada estado.
5. Eliminar de la carpeta temporal los originales sensibles cuando ya no sean
   necesarios.
6. Revisar `git status` antes de cualquier commit.

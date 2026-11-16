# Checklist de evidencias

Marcar una casilla únicamente después de revisar el archivo final y actualizar
su estado en `MATRIZ_EVIDENCIAS.md`.

## Preparación y privacidad

- [ ] Definir nombres, correos y propiedades ficticias para la ronda final.
- [ ] Confirmar que ninguna cuenta use una contraseña personal o reutilizada.
- [ ] Activar modo de no molestar en la tablet.
- [ ] Preparar una carpeta privada temporal para originales sin redactar.
- [ ] Confirmar que las capturas finales no muestren contraseñas, códigos, JWT,
      hashes, cabeceras o correos privados.
- [ ] Confirmar fecha y dispositivo utilizados.

## Tablet — autenticación y RNF03

- [ ] E01 Registro público.
- [ ] E02 Login propietario.
- [ ] E03 Login arrendatario.
- [ ] E04 Restauración de sesión y logout.
- [ ] E05 Respuesta neutral de recuperación.
- [ ] E06 Restablecimiento confirmado sin mostrar código ni contraseña.

## Tablet — propietario y fotografías

- [ ] E07 Panel propietario.
- [ ] E08 Publicación de inmueble temporal.
- [ ] E09 Edición y regreso a Mis inmuebles.
- [ ] E10 Eliminación controlada de inmueble temporal.
- [ ] E11 Selección y carga de fotografías.
- [ ] E12 Fotografías reales y cambio de portada.

## Tablet — arrendatario y relación de arriendo

- [ ] E13 Propiedades disponibles.
- [ ] E14 Detalle y galería.
- [ ] E15 Solicitud enviada.
- [ ] E16 Solicitud respondida por propietario.
- [ ] E17 Contrato creado.
- [ ] E18 Contrato visible para ambos roles.
- [ ] E19 Pago e historial.

## Tablet — módulos complementarios

- [ ] E20 Notificación, acción Ver y estado leído.
- [ ] E21 Conversación y mensajes ficticios.
- [ ] E22 Falla reportada por arrendatario.
- [ ] E23 Falla gestionada por propietario.
- [ ] E24 Reporte de ingresos.
- [ ] E25 Panel administrador.
- [ ] E26 Alta o edición de usuario temporal.
- [ ] E27 FAQ local con búsqueda y respuesta desplegada.

## Backend, seguridad y base de datos

- [ ] E28 Estado público del backend.
- [ ] E29 Respuestas saneadas 401 y 403.
- [x] E30 Fuente JWT y autorización actual disponible.
- [x] E31 Fuente Android Keystore/AES-GCM disponible.
- [ ] E32 Conteos bcrypt agregados.
- [ ] E33 Inventario de diez tablas.
- [ ] E34 Índices y claves foráneas.
- [ ] E35 Respaldo conjunto restaurado y verificado.

## Pruebas, APK y GitHub

- [ ] E36 Suite backend registrada con fecha.
- [ ] E37 Suite Android registrada con fecha.
- [ ] E38 `assembleDebug` registrado con fecha.
- [ ] E39 SHA-256 y tamaño del APK.
- [ ] E40 Rama, commit y estado limpio.
- [ ] E41 Repositorio GitHub final.
- [x] E42 Documentación y ejemplos seguros disponibles.
- [x] E43 Migraciones protegidas disponibles.

## Revisión final

- [ ] Todos los nombres coinciden con la matriz.
- [ ] No hay archivos vacíos, duplicados o ilegibles.
- [ ] Las capturas muestran fecha o están acompañadas por un registro fechado.
- [ ] Los datos temporales están identificados.
- [ ] Las cuentas, propiedades y registros temporales autorizados fueron
      eliminados al terminar.
- [ ] No se incluyeron APK, dumps, fotografías privadas ni archivos sin
      redactar en Git.
- [ ] La matriz refleja el estado real final.
- [ ] Se generó un índice o PDF de entrega a partir de las evidencias revisadas.

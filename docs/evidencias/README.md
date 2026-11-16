# Evidencias finales de EVOHKA

Esta carpeta organiza las evidencias de entrega del proyecto SENA ADSO. Los
archivos aquí preparados son una guía y un inventario: no representan capturas
ni resultados que todavía no se hayan obtenido.

## Estructura

```text
docs/evidencias/
├── MATRIZ_EVIDENCIAS.md
├── CHECKLIST.md
├── GUIA_CAPTURA.md
├── 01_tablet/
├── 02_backend_api/
├── 03_base_datos/
├── 04_pruebas_compilacion/
├── 05_seguridad_rnf03/
└── 06_git_entrega/
```

## Convención de nombres

Usar nombres en minúsculas, sin espacios ni tildes:

```text
evohka_<codigo>_<numero>_<descripcion>_<AAAA-MM-DD>.<extension>
```

Ejemplo ficticio:

```text
evohka_rf01_01_publicacion_inmueble_2026-11-20.png
```

No cambiar un estado a `Disponible` hasta que el archivo exista, sea legible y
haya sido revisado para eliminar datos sensibles.

## Formatos recomendados

- Capturas: PNG.
- Recorridos cortos: MP4, solo si la evidencia estática no demuestra el flujo.
- Resultados de comandos: TXT o PNG.
- Informes: Markdown o PDF generado desde documentación revisada.
- APK: conservar fuera de Git; registrar nombre, versión y SHA-256 en un TXT.

## Reglas de seguridad

- Utilizar únicamente cuentas y datos ficticios claramente identificables.
- No capturar contraseñas, códigos de recuperación, JWT, cabeceras
  `Authorization`, peppers, hashes completos, `.env` ni
  `evohka.properties`.
- Ocultar notificaciones del sistema, correos personales, IP pública y rutas
  privadas innecesarias.
- En MySQL mostrar estructura y conteos agregados, nunca la columna
  `contrasena` ni datos personales completos.
- En Postman o PowerShell ocultar cabeceras y cuerpos que contengan tokens.
- No versionar APK, dumps SQL, fotografías reales ni material sin redactar.
- Revisar cada archivo antes de agregarlo a GitHub.

## Estado inicial

Las funcionalidades y pruebas indicadas en la documentación fueron validadas
previamente, pero todavía no existen en esta carpeta informes fechados que las
demuestren. Por eso la mayoría de evidencias operativas comienzan como
`Pendiente`.

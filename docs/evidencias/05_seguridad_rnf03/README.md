# Evidencias de seguridad RNF03

Las evidencias pueden referenciar código y resultados agregados de:

- bcrypt y `contrasena_algoritmo`;
- JWT HS256, issuer, audience, expiración y versión de sesión;
- autorización por rol actual;
- recuperación con código temporal, uso único y revocación;
- Android Keystore y AES-GCM;
- exclusión de sesión de Auto Backup;
- manejo Android de HTTP 401, 403 y errores de red.

No capturar secretos, tokens, hashes completos, contraseñas, peppers ni códigos
de recuperación. Preferir fragmentos de código con números de línea y conteos
agregados.

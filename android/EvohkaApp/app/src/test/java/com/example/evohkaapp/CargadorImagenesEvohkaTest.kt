package com.example.evohkaapp

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class CargadorImagenesEvohkaTest {
    @Test
    fun agregaJwtALaSolicitudProtegida() {
        val solicitud = Request.Builder()
            .url("http://evohka.test/api/propiedades/13/fotos/1/archivo")
            .build()

        val autorizada = solicitud.conAutorizacionFotografia(
            "Bearer token-ficticio"
        )

        assertEquals(
            "Bearer token-ficticio",
            autorizada.header("Authorization")
        )
    }

    @Test
    fun noInventaAutorizacionCuandoNoHaySesion() {
        val solicitud = Request.Builder()
            .url("http://evohka.test/api/propiedades/13/fotos/1/archivo")
            .build()

        val sinSesion = solicitud.conAutorizacionFotografia(null)

        assertNull(sinSesion.header("Authorization"))
    }

    @Test
    fun informaCodigoHttpSinExponerLaRespuesta() {
        assertEquals(
            "HTTP 401",
            describirFalloFotografia(
                EstadoDescargaFotografia(
                    codigoHttp = 401,
                    tipoContenido = "application/json"
                )
            )
        )
    }

    @Test
    fun distingueRespuestaJsonDeUnaImagen() {
        assertEquals(
            "La respuesta no es una imagen (HTTP 200, application/json)",
            describirFalloFotografia(
                EstadoDescargaFotografia(
                    codigoHttp = 200,
                    tipoContenido = "application/json"
                )
            )
        )
    }

    @Test
    fun identificaFalloDeDecodificacionWebp() {
        assertEquals(
            "Error de decodificación (HTTP 200, image/webp, 87724 bytes)",
            describirFalloFotografia(
                EstadoDescargaFotografia(
                    codigoHttp = 200,
                    tipoContenido = "image/webp",
                    tamanoBytes = 87724
                )
            )
        )
    }

    @Test
    fun identificaErrorDeRed() {
        assertEquals(
            "Error de red",
            describirFalloFotografia(
                EstadoDescargaFotografia(errorRed = true)
            )
        )
    }

    @Test
    fun muestraLaExcepcionRealYLaCausaRaizSinExponerUrl() {
        val error = IllegalStateException(
            "No se pudo cargar https://servidor.local/foto",
            IllegalArgumentException("Tipo de datos incompatible")
        )

        val diagnostico = describirFalloFotografia(null, error)

        assertTrue(
            diagnostico.startsWith("Error antes de la descarga HTTP")
        )
        assertTrue(diagnostico.contains("excepción=IllegalStateException"))
        assertTrue(diagnostico.contains("causa=IllegalArgumentException"))
        assertTrue(diagnostico.contains("[url]"))
        assertFalse(diagnostico.contains("servidor.local"))
    }

    @Test
    fun usaLaRutaApiComoClaveAunqueCambieElServidor() {
        assertEquals(
            "/api/propiedades/13/fotos/2/archivo",
            claveDiagnosticoFotografia(
                "http://192.168.40.43:3000/api/propiedades/13/fotos/2/archivo"
            )
        )
    }

    @Test
    fun ocultaRutasPrivadasEnElMensajeTecnico() {
        assertEquals(
            "Fallo en [ruta]",
            mensajeSeguroFotografia(
                "Fallo en C:\\Users\\persona\\archivo.webp"
            )
        )
        assertEquals(
            "Fallo en [ruta]",
            mensajeSeguroFotografia(
                "Fallo en /data/user/0/evohka/cache/archivo.webp"
            )
        )
    }

    @Test
    fun elFalloSincronoIdentificaLaEtapaYLaExcepcion() {
        val diagnostico = describirFalloFotografia(
            estado = null,
            error = IllegalArgumentException("Datos incompatibles"),
            etapaSinRespuestaHttp = "Fallo al iniciar ImageRequest"
        )

        assertTrue(diagnostico.startsWith("Fallo al iniciar ImageRequest"))
        assertTrue(diagnostico.contains("excepción=IllegalArgumentException"))
        assertTrue(diagnostico.contains("detalle=Datos incompatibles"))
        assertFalse(diagnostico.contains("Error desconocido"))
    }

    @Test
    fun identificaElResourceIdQueImpidioIniciarCoil() {
        val diagnostico = describirFalloFotografia(
            estado = null,
            error = IllegalStateException(
                "Invalid resource ID: 2131165347"
            ),
            etapaSinRespuestaHttp = "Fallo al iniciar ImageRequest"
        )

        assertTrue(diagnostico.contains("excepción=IllegalStateException"))
        assertTrue(diagnostico.contains("2131165347"))
    }
}

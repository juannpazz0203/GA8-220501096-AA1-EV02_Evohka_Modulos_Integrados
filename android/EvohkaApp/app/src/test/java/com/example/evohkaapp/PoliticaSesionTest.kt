package com.example.evohkaapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaSesionTest {

    @Test
    fun restauracionCon401LimpiaSesion() {
        assertEquals(
            AccionRestauracionSesion.LIMPIAR_Y_SOLICITAR_LOGIN,
            PoliticaRestauracionSesion.paraCodigoHttp(401)
        )
    }

    @Test
    fun restauracionCon403ConservaSesion() {
        assertEquals(
            AccionRestauracionSesion.CONSERVAR_Y_REINTENTAR,
            PoliticaRestauracionSesion.paraCodigoHttp(403)
        )
    }

    @Test
    fun erroresDeServidorYRedConservanSesion() {
        assertEquals(
            AccionRestauracionSesion.CONSERVAR_Y_REINTENTAR,
            PoliticaRestauracionSesion.paraCodigoHttp(503)
        )
        assertEquals(
            AccionRestauracionSesion.CONSERVAR_Y_REINTENTAR,
            PoliticaRestauracionSesion.paraErrorDeRed()
        )
    }

    @Test
    fun interceptorSoloInvalida401DeSolicitudAutenticadaNormal() {
        assertTrue(
            PoliticaRespuestaSesion.debeInvalidar(
                codigoHttp = 401,
                tieneAutorizacion = true,
                esValidacionInicial = false
            )
        )
        assertFalse(
            PoliticaRespuestaSesion.debeInvalidar(403, true, false)
        )
        assertFalse(
            PoliticaRespuestaSesion.debeInvalidar(500, true, false)
        )
        assertFalse(
            PoliticaRespuestaSesion.debeInvalidar(401, false, false)
        )
        assertFalse(
            PoliticaRespuestaSesion.debeInvalidar(401, true, true)
        )
    }
}

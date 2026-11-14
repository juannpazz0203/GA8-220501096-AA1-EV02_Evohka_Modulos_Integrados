package com.example.evohkaapp

import org.junit.Assert.assertEquals
import org.junit.Test

class MisInmueblesSesionTest {
    @Test
    fun sesionValidadaConIdentidadCargaInmuebles() {
        assertEquals(
            AccionAccesoMisInmuebles.CARGAR,
            PoliticaAccesoMisInmuebles.decidir("Bearer prueba", 7)
        )
    }

    @Test
    fun tokenValidoSinIdentidadLaRecuperaDelBackend() {
        assertEquals(
            AccionAccesoMisInmuebles.RECUPERAR_IDENTIDAD,
            PoliticaAccesoMisInmuebles.decidir("Bearer prueba", 0)
        )
    }

    @Test
    fun procesoRecreadoSinTokenValidadoRestauraPorLogin() {
        assertEquals(
            AccionAccesoMisInmuebles.RESTAURAR_SESION,
            PoliticaAccesoMisInmuebles.decidir(null, 7)
        )
    }
}

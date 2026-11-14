package com.example.evohkaapp

import org.junit.Assert.assertEquals
import org.junit.Test

class PoliticaNavegacionFotografiasTest {
    @Test
    fun propiedadSinFotosRegresaAMisInmuebles() {
        assertEquals(
            DestinoPostGuardadoFotografias.MIS_INMUEBLES,
            PoliticaNavegacionFotografias.decidir(false, false)
        )
    }

    @Test
    fun fotosSubidasRegresanAMisInmuebles() {
        assertEquals(
            DestinoPostGuardadoFotografias.MIS_INMUEBLES,
            PoliticaNavegacionFotografias.decidir(true, true)
        )
    }

    @Test
    fun cargaFallidaPermaneceParaReintentar() {
        assertEquals(
            DestinoPostGuardadoFotografias.REINTENTAR_FOTOGRAFIAS,
            PoliticaNavegacionFotografias.decidir(true, false)
        )
    }

}

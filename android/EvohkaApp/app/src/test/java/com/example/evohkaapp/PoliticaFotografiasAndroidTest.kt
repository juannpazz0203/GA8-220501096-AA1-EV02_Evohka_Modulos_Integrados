package com.example.evohkaapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaFotografiasAndroidTest {
    @Test
    fun calculaCupoRestanteSinSuperarDiez() {
        assertEquals(10, PoliticaFotografiasAndroid.cupoDisponible(0, 0))
        assertEquals(3, PoliticaFotografiasAndroid.cupoDisponible(5, 2))
        assertEquals(0, PoliticaFotografiasAndroid.cupoDisponible(9, 2))
    }

    @Test
    fun admiteFormatosYLimiteAcordados() {
        assertNull(
            PoliticaFotografiasAndroid.motivoRechazo(
                "image/jpeg",
                PoliticaFotografiasAndroid.MAX_BYTES
            )
        )
        assertNull(
            PoliticaFotografiasAndroid.motivoRechazo(
                "image/png",
                1L
            )
        )
        assertNull(
            PoliticaFotografiasAndroid.motivoRechazo(
                "image/webp",
                null
            )
        )
    }

    @Test
    fun rechazaFormatosNoPermitidosYArchivosGrandes() {
        assertTrue(
            PoliticaFotografiasAndroid
                .motivoRechazo("image/gif", 100L)
                ?.contains("JPG") == true
        )
        assertTrue(
            PoliticaFotografiasAndroid.motivoRechazo(
                "image/jpeg",
                PoliticaFotografiasAndroid.MAX_BYTES + 1L
            )?.contains("15 MB") == true
        )
    }
}

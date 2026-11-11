package com.example.evohkaapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendUrlProviderTest {

    @Test
    fun seleccionaUrlEmuladorParaAndroidStudio() {
        val identidad = identidad(
            fingerprint = "google/sdk_gphone64_x86_64/emu64:15/test-keys",
            model = "sdk_gphone64_x86_64",
            brand = "google",
            product = "sdk_gphone64_x86_64",
            hardware = "ranchu"
        )

        assertTrue(BackendUrlProvider.esEmulador(identidad))
        assertEquals(
            "http://10.0.2.2:3000/",
            BackendUrlProvider.seleccionarBaseUrl(
                identidad,
                "http://10.0.2.2:3000/",
                "http://192.168.40.43:3000/"
            )
        )
    }

    @Test
    fun seleccionaUrlLanParaTabletFisica() {
        val identidad = identidad(
            fingerprint = "samsung/gta9wifi/gta9:14/release-keys",
            model = "SM-X110",
            manufacturer = "samsung",
            brand = "samsung",
            device = "gta9wifi",
            product = "gta9wifi",
            hardware = "mt6769"
        )

        assertFalse(BackendUrlProvider.esEmulador(identidad))
        assertEquals(
            "http://192.168.40.43:3000/",
            BackendUrlProvider.seleccionarBaseUrl(
                identidad,
                "http://10.0.2.2:3000/",
                "http://192.168.40.43:3000/"
            )
        )
    }

    private fun identidad(
        fingerprint: String = "physical/release-keys",
        model: String = "Tablet",
        manufacturer: String = "fabricante",
        brand: String = "marca",
        device: String = "tablet",
        product: String = "tablet",
        hardware: String = "hardware"
    ) = IdentidadDispositivoAndroid(
        fingerprint = fingerprint,
        model = model,
        manufacturer = manufacturer,
        brand = brand,
        device = device,
        product = product,
        hardware = hardware
    )
}

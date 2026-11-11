package com.example.evohkaapp

import android.os.Build

internal data class IdentidadDispositivoAndroid(
    val fingerprint: String,
    val model: String,
    val manufacturer: String,
    val brand: String,
    val device: String,
    val product: String,
    val hardware: String
)

internal object BackendUrlProvider {

    fun obtenerBaseUrl(): String {
        val identidad = IdentidadDispositivoAndroid(
            fingerprint = Build.FINGERPRINT,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            hardware = Build.HARDWARE
        )

        return seleccionarBaseUrl(
            identidad = identidad,
            urlEmulador = BuildConfig.EVOHKA_EMULATOR_BASE_URL,
            urlDispositivoFisico = BuildConfig.EVOHKA_PHYSICAL_BASE_URL
        )
    }

    internal fun seleccionarBaseUrl(
        identidad: IdentidadDispositivoAndroid,
        urlEmulador: String,
        urlDispositivoFisico: String
    ): String {
        val url = if (esEmulador(identidad)) {
            urlEmulador
        } else {
            urlDispositivoFisico
        }

        require(
            (url.startsWith("http://") || url.startsWith("https://")) &&
                url.endsWith("/")
        ) {
            "La URL base de EVOHKA no es válida"
        }

        return url
    }

    internal fun esEmulador(
        identidad: IdentidadDispositivoAndroid
    ): Boolean {
        val fingerprint = identidad.fingerprint.lowercase()
        val model = identidad.model.lowercase()
        val manufacturer = identidad.manufacturer.lowercase()
        val brand = identidad.brand.lowercase()
        val device = identidad.device.lowercase()
        val product = identidad.product.lowercase()
        val hardware = identidad.hardware.lowercase()

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            manufacturer.contains("genymotion") ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product == "sdk" ||
            product.startsWith("sdk_") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu")
    }
}

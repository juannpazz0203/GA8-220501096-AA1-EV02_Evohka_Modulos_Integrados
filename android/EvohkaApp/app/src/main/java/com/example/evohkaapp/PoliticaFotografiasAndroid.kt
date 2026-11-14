package com.example.evohkaapp

internal object PoliticaFotografiasAndroid {
    const val MAX_FOTOGRAFIAS = 10
    const val MAX_BYTES = 15L * 1024L * 1024L

    private val tiposAdmitidos = setOf(
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    fun cupoDisponible(
        fotografiasExistentes: Int,
        seleccionadas: Int
    ): Int =
        (MAX_FOTOGRAFIAS - fotografiasExistentes - seleccionadas)
            .coerceAtLeast(0)

    fun motivoRechazo(
        mimeType: String?,
        tamanoBytes: Long?
    ): String? {
        if (mimeType?.lowercase() !in tiposAdmitidos) {
            return "Solo se admiten imágenes JPG, PNG o WebP"
        }
        if (tamanoBytes != null && tamanoBytes > MAX_BYTES) {
            return "Cada fotografía debe pesar como máximo 15 MB"
        }
        if (tamanoBytes != null && tamanoBytes <= 0L) {
            return "La fotografía seleccionada está vacía"
        }
        return null
    }
}

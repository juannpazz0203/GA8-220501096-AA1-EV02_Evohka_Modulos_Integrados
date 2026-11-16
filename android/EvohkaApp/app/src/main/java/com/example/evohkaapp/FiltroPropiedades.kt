package com.example.evohkaapp

import java.text.Normalizer

internal data class CriteriosFiltroPropiedades(
    val texto: String = "",
    val ciudad: String? = null,
    val tipoInmueble: String? = null,
    val precioMinimo: Double? = null,
    val precioMaximo: Double? = null
) {
    fun tieneFiltrosActivos(): Boolean =
        texto.isNotBlank() ||
            !ciudad.isNullOrBlank() ||
            !tipoInmueble.isNullOrBlank() ||
            precioMinimo != null ||
            precioMaximo != null
}

internal data class ResultadoRangoPrecio(
    val minimo: Double? = null,
    val maximo: Double? = null,
    val mensajeError: String? = null
) {
    val esValido: Boolean
        get() = mensajeError == null
}

internal object FiltroPropiedades {

    fun filtrar(
        propiedades: List<Propiedad>,
        criterios: CriteriosFiltroPropiedades
    ): List<Propiedad> {
        val texto = normalizar(criterios.texto)
        val ciudad = normalizar(criterios.ciudad)
        val tipo = normalizar(criterios.tipoInmueble)
        val precioMinimo = criterios.precioMinimo
        val precioMaximo = criterios.precioMaximo

        return propiedades.filter { propiedad ->
            val coincideTexto = texto.isEmpty() || listOf(
                propiedad.titulo,
                propiedad.ciudad,
                propiedad.barrio
            ).any { valor -> normalizar(valor).contains(texto) }

            val coincideCiudad = ciudad.isEmpty() ||
                normalizar(propiedad.ciudad) == ciudad

            val coincideTipo = tipo.isEmpty() ||
                normalizar(propiedad.tipo_inmueble) == tipo

            val canon = propiedad.canon
                ?.takeIf { it.isFinite() && it >= 0.0 }
            val coincideMinimo = precioMinimo == null ||
                canon?.let { it >= precioMinimo } == true
            val coincideMaximo = precioMaximo == null ||
                canon?.let { it <= precioMaximo } == true

            coincideTexto &&
                coincideCiudad &&
                coincideTipo &&
                coincideMinimo &&
                coincideMaximo
        }
    }

    fun ciudadesDisponibles(
        propiedades: List<Propiedad>
    ): List<String> = opcionesDisponibles(
        propiedades.map { it.ciudad }
    )

    fun tiposDisponibles(
        propiedades: List<Propiedad>
    ): List<String> = opcionesDisponibles(
        propiedades.map { it.tipo_inmueble }
    )

    fun validarRangoPrecio(
        minimoTexto: String,
        maximoTexto: String
    ): ResultadoRangoPrecio {
        val minimo = convertirPrecio(minimoTexto)
        if (minimoTexto.isNotBlank() && minimo == null) {
            return ResultadoRangoPrecio(
                mensajeError = "El precio mínimo no es válido"
            )
        }

        val maximo = convertirPrecio(maximoTexto)
        if (maximoTexto.isNotBlank() && maximo == null) {
            return ResultadoRangoPrecio(
                mensajeError = "El precio máximo no es válido"
            )
        }

        if (minimo != null && maximo != null && minimo > maximo) {
            return ResultadoRangoPrecio(
                minimo = minimo,
                maximo = maximo,
                mensajeError = "El precio mínimo no puede superar el máximo"
            )
        }

        return ResultadoRangoPrecio(
            minimo = minimo,
            maximo = maximo
        )
    }

    private fun convertirPrecio(texto: String): Double? {
        if (texto.isBlank()) return null

        return texto
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }
    }

    private fun opcionesDisponibles(
        valores: List<String?>
    ): List<String> {
        val porValorNormalizado = linkedMapOf<String, String>()

        valores.forEach { valor ->
            val visible = valor?.trim().orEmpty()
            val normalizado = normalizar(visible)
            if (normalizado.isNotEmpty() && normalizado !in porValorNormalizado) {
                porValorNormalizado[normalizado] = visible
            }
        }

        return porValorNormalizado
            .values
            .sortedBy { normalizar(it) }
    }

    private fun normalizar(valor: String?): String {
        if (valor.isNullOrBlank()) return ""

        return Normalizer.normalize(
            valor.trim().lowercase(),
            Normalizer.Form.NFD
        ).replace("\\p{Mn}+".toRegex(), "")
    }
}

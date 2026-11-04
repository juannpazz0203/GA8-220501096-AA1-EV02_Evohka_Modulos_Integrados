package com.example.evohkaapp

import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatoEvohka {

    private val localeColombia =
        Locale.forLanguageTag("es-CO")

    fun moneda(valor: Double?): String {
        return NumberFormat.getCurrencyInstance(
            localeColombia
        ).format(valor ?: 0.0)
    }

    fun fecha(valor: String?): String {
        if (valor.isNullOrBlank()) {
            return "No disponible"
        }

        val original = valor.trim()
        val fechaBase =
            Regex("^\\d{4}-\\d{2}-\\d{2}")
                .find(original)
                ?.value
                ?: return original

        return try {
            val fecha =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).apply {
                    isLenient = false
                }.parse(fechaBase)
                    ?: return original

            SimpleDateFormat(
                "dd/MM/yyyy",
                localeColombia
            ).format(fecha)
        } catch (error: ParseException) {
            original
        }
    }

    fun fechaHora(valor: String?): String {
        if (valor.isNullOrBlank()) {
            return "No disponible"
        }

        val original = valor.trim()
        val coincidencia =
            Regex(
                "^(\\d{4}-\\d{2}-\\d{2})[T ](\\d{2}:\\d{2})"
            ).find(original)
                ?: return fecha(original)

        return "${fecha(coincidencia.groupValues[1])} ${coincidencia.groupValues[2]}"
    }

    fun fechaVisible(fecha: Date): String {
        return SimpleDateFormat(
            "dd/MM/yyyy",
            localeColombia
        ).format(fecha)
    }

    fun fechaIso(fecha: Date): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(fecha)
    }
}

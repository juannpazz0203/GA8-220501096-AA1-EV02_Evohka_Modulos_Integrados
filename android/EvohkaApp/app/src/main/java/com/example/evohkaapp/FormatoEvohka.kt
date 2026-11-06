package com.example.evohkaapp

import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    fun fechaHoraLocal(valor: String?): String {
        if (valor.isNullOrBlank()) {
            return "No disponible"
        }

        val original = valor.trim()
        val coincidencia = Regex(
            "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})" +
                "(?:\\.(\\d{1,9}))?(Z|[+-]\\d{2}:?\\d{2})$"
        ).find(original) ?: return fechaHora(original)

        val milisegundos = coincidencia.groupValues[2]
            .padEnd(3, '0')
            .take(3)
        val zona = coincidencia.groupValues[3].let {
            if (it == "Z") "+0000" else it.replace(":", "")
        }
        val normalizada =
            "${coincidencia.groupValues[1]}.$milisegundos$zona"

        return try {
            val instante = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                Locale.US
            ).apply {
                isLenient = false
            }.parse(normalizada) ?: return fechaHora(original)

            SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                localeColombia
            ).apply {
                timeZone = TimeZone.getDefault()
            }.format(instante)
        } catch (error: ParseException) {
            fechaHora(original)
        }
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

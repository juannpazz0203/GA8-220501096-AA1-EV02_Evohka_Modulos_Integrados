package com.example.evohkaapp

import android.graphics.Color
import android.widget.TextView

object EvohkaUi {

    fun aplicarEstado(
        vista: TextView,
        estado: String?
    ) {
        val normalizado = estado
            ?.trim()
            ?.lowercase()
            .orEmpty()

        val (fondo, color) = when (normalizado) {
            "activo", "aprobada", "pagado", "resuelta" ->
                R.drawable.bg_chip_success_evohka to "#2E7D32"

            "cancelado", "rechazada", "fallido" ->
                R.drawable.bg_chip_danger_evohka to "#C62828"

            "en_proceso", "en proceso" ->
                R.drawable.bg_chip_progress_evohka to "#7B2FF7"

            "finalizado", "finalizada" ->
                R.drawable.bg_chip_progress_evohka to "#5F5570"

            else ->
                R.drawable.bg_chip_pending_evohka to "#8A5700"
        }

        vista.setBackgroundResource(fondo)
        vista.setTextColor(Color.parseColor(color))
        vista.setPadding(
            dp(vista, 12),
            dp(vista, 7),
            dp(vista, 12),
            dp(vista, 7)
        )
    }

    private fun dp(vista: TextView, valor: Int): Int {
        return (valor * vista.resources.displayMetrics.density)
            .toInt()
    }
}

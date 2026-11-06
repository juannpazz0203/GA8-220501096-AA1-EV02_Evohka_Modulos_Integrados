package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.FrameLayout
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object NavegacionEvohka {

    enum class DestinoPropietario {
        INICIO,
        INMUEBLES,
        SOLICITUDES,
        CONTRATOS,
        NOTIFICACIONES
    }

    enum class DestinoArrendatario {
        INICIO,
        CONTRATOS,
        PAGOS,
        NOTIFICACIONES
    }

    fun configurarPropietario(
        activity: Activity,
        actual: DestinoPropietario?
    ) {
        configurar(
            activity,
            actual,
            listOf(
                Opcion(
                    R.id.navPropietarioInicio,
                    DestinoPropietario.INICIO,
                    InicioPropietarioActivity::class.java
                ),
                Opcion(
                    R.id.navPropietarioInmuebles,
                    DestinoPropietario.INMUEBLES,
                    MisInmueblesActivity::class.java
                ),
                Opcion(
                    R.id.navPropietarioSolicitudes,
                    DestinoPropietario.SOLICITUDES,
                    SolicitudesActivity::class.java
                ),
                Opcion(
                    R.id.navPropietarioContratos,
                    DestinoPropietario.CONTRATOS,
                    ContratosActivity::class.java
                ),
                Opcion(
                    R.id.navPropietarioNotificaciones,
                    DestinoPropietario.NOTIFICACIONES,
                    NotificacionesActivity::class.java
                )
            )
        )
    }

    fun configurarArrendatario(
        activity: Activity,
        actual: DestinoArrendatario?
    ) {
        configurar(
            activity,
            actual,
            listOf(
                Opcion(
                    R.id.navArrendatarioInicio,
                    DestinoArrendatario.INICIO,
                    PropiedadesDisponiblesActivity::class.java
                ),
                Opcion(
                    R.id.navArrendatarioContratos,
                    DestinoArrendatario.CONTRATOS,
                    ContratosActivity::class.java
                ),
                Opcion(
                    R.id.navArrendatarioPagos,
                    DestinoArrendatario.PAGOS,
                    PagosActivity::class.java
                ),
                Opcion(
                    R.id.navArrendatarioNotificaciones,
                    DestinoArrendatario.NOTIFICACIONES,
                    NotificacionesActivity::class.java
                )
            )
        )
    }

    fun configurarSegunRol(
        activity: Activity,
        idContenedor: Int,
        destinoPropietario: DestinoPropietario?,
        destinoArrendatario: DestinoArrendatario?
    ) {
        val contenedor =
            activity.findViewById<FrameLayout>(
                idContenedor
            )

        val rol = activity.getSharedPreferences(
            "EvohkaSesion",
            Activity.MODE_PRIVATE
        ).getString("rol", null)

        val layout = when (rol) {
            "propietario" ->
                R.layout.view_navegacion_propietario

            "arrendatario" ->
                R.layout.view_navegacion_arrendatario

            else ->
                return
        }

        activity.layoutInflater.inflate(
            layout,
            contenedor,
            true
        )

        when (rol) {
            "propietario" ->
                configurarPropietario(
                    activity,
                    destinoPropietario
                )

            "arrendatario" ->
                configurarArrendatario(
                    activity,
                    destinoArrendatario
                )
        }
    }

    private fun <T> configurar(
        activity: Activity,
        actual: T?,
        opciones: List<Opcion<T>>
    ) {
        opciones.forEach { opcion ->
            val vista = activity.findViewById<TextView>(
                opcion.idVista
            ) ?: return@forEach

            val activa = opcion.destino == actual

            vista.setTextColor(
                Color.parseColor(
                    if (activa) "#CE74FF" else "#5F5862"
                )
            )

            vista.typeface = if (activa) {
                Typeface.DEFAULT_BOLD
            } else {
                Typeface.create(
                    "sans-serif-medium",
                    Typeface.NORMAL
                )
            }
            vista.setBackgroundResource(
                if (activa) {
                    R.drawable.bg_nav_item_active
                } else {
                    android.R.color.transparent
                }
            )
            vista.alpha = if (activa) 1f else 0.95f
            vista.setPadding(
                dp(vista, 4),
                dp(vista, 3),
                dp(vista, 4),
                dp(vista, 3)
            )

            vista.isSelected = activa
            vista.setOnClickListener {
                if (!activa) {
                    val intent = Intent(
                        activity,
                        opcion.activityDestino
                    ).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }

                    activity.startActivity(intent)
                }
            }
        }
    }

    fun actualizarIndicadorNotificaciones(activity: Activity) {
        val vista = vistaNotificaciones(activity) ?: return

        if (activity is NotificacionesActivity) {
            return
        }

        val token = activity.getSharedPreferences(
            "EvohkaSesion",
            Activity.MODE_PRIVATE
        ).getString("token", null)

        if (token.isNullOrBlank()) {
            mostrarIndicador(vista, 0)
            return
        }

        RetrofitClient.apiService.listarNotificaciones(token).enqueue(
            object : Callback<RespuestaNotificaciones> {
                override fun onResponse(
                    call: Call<RespuestaNotificaciones>,
                    response: Response<RespuestaNotificaciones>
                ) {
                    if (response.isSuccessful) {
                        mostrarIndicador(
                            vista,
                            response.body()?.no_leidas ?: 0
                        )
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaNotificaciones>,
                    t: Throwable
                ) = Unit
            }
        )
    }

    fun mostrarIndicadorNotificaciones(
        activity: Activity,
        noLeidas: Int
    ) {
        vistaNotificaciones(activity)?.let {
            mostrarIndicador(it, noLeidas)
        }
    }

    private fun vistaNotificaciones(activity: Activity): TextView? {
        return activity.findViewById<TextView>(
            R.id.navPropietarioNotificaciones
        ) ?: activity.findViewById(
            R.id.navArrendatarioNotificaciones
        )
    }

    private fun mostrarIndicador(
        vista: TextView,
        cantidad: Int
    ) {
        val pendientes = cantidad.coerceAtLeast(0)
        val primeraLinea = if (pendientes > 0) {
            "● ${if (pendientes > 99) "99+" else pendientes}"
        } else {
            "○"
        }
        val contenido = SpannableString("$primeraLinea\nAlertas")

        if (pendientes > 0) {
            contenido.setSpan(
                ForegroundColorSpan(Color.parseColor("#C62828")),
                0,
                primeraLinea.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            contenido.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                primeraLinea.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        vista.text = contenido
        vista.contentDescription = if (pendientes > 0) {
            "Alertas, $pendientes sin leer"
        } else {
            "Alertas, ninguna sin leer"
        }
    }

    private data class Opcion<T>(
        val idVista: Int,
        val destino: T,
        val activityDestino: Class<out Activity>
    )

    private fun dp(vista: TextView, valor: Int): Int {
        return (valor * vista.resources.displayMetrics.density)
            .toInt()
    }
}

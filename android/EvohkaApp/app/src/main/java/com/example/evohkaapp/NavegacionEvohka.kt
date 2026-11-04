package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.TextView

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
                    if (activa) "#CE74FF" else "#888888"
                )
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

    private data class Opcion<T>(
        val idVista: Int,
        val destino: T,
        val activityDestino: Class<out Activity>
    )
}

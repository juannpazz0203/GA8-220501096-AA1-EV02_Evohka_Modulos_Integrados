package com.example.evohkaapp

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class SolicitudesActivity : Activity() {

    private lateinit var txtEstadoSolicitudes: TextView
    private lateinit var contenedorSolicitudes: LinearLayout
    private var actualizandoEstado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_solicitudes)

        txtEstadoSolicitudes =
            findViewById(R.id.txtEstadoSolicitudes)

        contenedorSolicitudes =
            findViewById(R.id.contenedorSolicitudes)

        val btnVolver =
            findViewById<Button>(R.id.btnVolverSolicitudes)

        btnVolver.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarSolicitudes()
    }

    private fun dp(valor: Int): Int {
        return (
            valor * resources.displayMetrics.density
        ).toInt()
    }

    private fun cargarSolicitudes() {
        val preferencias =
            getSharedPreferences(
                "EvohkaSesion",
                MODE_PRIVATE
            )

        val token =
            preferencias.getString(
                "token",
                null
            )

        val rol =
            preferencias.getString(
                "rol",
                null
            )

        contenedorSolicitudes.removeAllViews()

        if (token.isNullOrBlank()) {
            txtEstadoSolicitudes.text =
                "Sesión no válida"

            Toast.makeText(
                this,
                "Debe iniciar sesión nuevamente",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (rol != "propietario") {
            txtEstadoSolicitudes.text =
                "Acceso no autorizado"

            return
        }

        txtEstadoSolicitudes.text =
            "Consultando solicitudes..."

        RetrofitClient.apiService
            .listarSolicitudesRecibidas(token)
            .enqueue(
                object : Callback<RespuestaSolicitudes> {

                    override fun onResponse(
                        call: Call<RespuestaSolicitudes>,
                        response: Response<RespuestaSolicitudes>
                    ) {
                        if (response.isSuccessful) {
                            mostrarSolicitudes(
                                response.body()?.datos
                                    ?: emptyList()
                            )
                        } else {
                            txtEstadoSolicitudes.text =
                                "No se pudieron consultar las solicitudes"

                            Toast.makeText(
                                this@SolicitudesActivity,
                                obtenerMensajeError(
                                    response,
                                    "No se pudieron consultar las solicitudes"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RespuestaSolicitudes>,
                        t: Throwable
                    ) {
                        txtEstadoSolicitudes.text =
                            "Error de conexión"

                        Toast.makeText(
                            this@SolicitudesActivity,
                            "No se pudo conectar con el servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    private fun mostrarSolicitudes(
        solicitudes: List<Solicitud>
    ) {
        contenedorSolicitudes.removeAllViews()

        if (solicitudes.isEmpty()) {
            txtEstadoSolicitudes.text =
                "No tienes solicitudes recibidas"

            return
        }

        txtEstadoSolicitudes.text =
            "${solicitudes.size} solicitud(es) recibida(s)"

        solicitudes.forEach { solicitud ->
            agregarTarjetaSolicitud(solicitud)
        }
    }

    private fun agregarTarjetaSolicitud(
        solicitud: Solicitud
    ) {
        val tarjeta =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(16),
                    dp(18),
                    dp(16)
                )

                setBackgroundResource(
                    R.drawable.bg_card_evohka
                )

                elevation =
                    dp(2).toFloat()
            }

        tarjeta.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    dp(16)
                )
            }

        val nombreSolicitante =
            listOfNotNull(
                solicitud.nombre_solicitante
                    ?.takeIf { it.isNotBlank() },
                solicitud.apellido_solicitante
                    ?.takeIf { it.isNotBlank() }
            ).joinToString(" ")
                .ifBlank {
                    "Solicitante"
                }

        val txtSolicitante =
            TextView(this).apply {
                text = nombreSolicitante
                textSize = 18f

                setTextColor(
                    Color.parseColor("#202020")
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        tarjeta.addView(txtSolicitante)

        val txtPropiedad =
            TextView(this).apply {
                text =
                    "Interesado en: ${solicitud.titulo_propiedad ?: "Inmueble"}"

                textSize = 15f

                setTextColor(
                    Color.parseColor("#333333")
                )

                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(4)
                )
            }

        tarjeta.addView(txtPropiedad)

        val txtFecha =
            TextView(this).apply {
                text =
                    "Fecha de solicitud: ${formatearFechaSolicitud(solicitud.fecha_solicitud)}"

                textSize = 14f

                setTextColor(
                    Color.parseColor("#666666")
                )
            }

        tarjeta.addView(txtFecha)

        val estadoNormalizado =
            solicitud.estado
                ?.trim()
                ?.lowercase()
                ?: "sin estado"

        val txtEstado =
            TextView(this).apply {
                text =
                    "Estado: ${etiquetaEstado(estadoNormalizado)}"

                textSize = 15f

                setTextColor(
                    colorEstado(estadoNormalizado)
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setBackgroundResource(
                    R.drawable.bg_input_evohka
                )

                setPadding(
                    dp(12),
                    dp(8),
                    dp(12),
                    dp(8)
                )
            }

        tarjeta.addView(
            txtEstado,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    0,
                    dp(12),
                    0,
                    0
                )
            }
        )

        val observaciones =
            solicitud.observaciones
                ?.trim()

        if (!observaciones.isNullOrEmpty()) {
            val txtObservaciones =
                TextView(this).apply {
                    text =
                        "Observaciones:\n$observaciones"

                    textSize = 14f

                    setTextColor(
                        Color.parseColor("#202020")
                    )

                    setBackgroundResource(
                        R.drawable.bg_input_evohka
                    )

                    setPadding(
                        dp(14),
                        dp(12),
                        dp(14),
                        dp(12)
                    )
                }

            tarjeta.addView(
                txtObservaciones,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        0,
                        dp(12),
                        0,
                        0
                    )
                }
            )
        }

        if (estadoNormalizado == "pendiente") {
            tarjeta.addView(
                crearAcciones(solicitud),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        0,
                        dp(14),
                        0,
                        0
                    )
                }
            )
        }

        contenedorSolicitudes.addView(tarjeta)
    }

    private fun formatearFechaSolicitud(
        valorFecha: String?
    ): String {
        if (valorFecha.isNullOrBlank()) {
            return "No disponible"
        }

        val valorOriginal =
            valorFecha.trim()

        val fechaBase =
            Regex("^\\d{4}-\\d{2}-\\d{2}")
                .find(valorOriginal)
                ?.value
                ?: return valorOriginal

        return try {
            val fecha =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).apply {
                    isLenient = false
                }.parse(fechaBase)
                    ?: return valorOriginal

            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.forLanguageTag("es-CO")
            ).format(fecha)
        } catch (error: ParseException) {
            valorOriginal
        }
    }

    private fun crearAcciones(
        solicitud: Solicitud
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER

            val btnAprobar =
                Button(this@SolicitudesActivity).apply {
                    text = "Aprobar"
                    isAllCaps = false

                    setTextColor(
                        Color.parseColor("#170E0E")
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    setBackgroundResource(
                        R.drawable.bg_button_evohka
                    )

                    backgroundTintList = null

                    setOnClickListener {
                        confirmarRespuesta(
                            solicitud,
                            "aprobada"
                        )
                    }
                }

            addView(
                btnAprobar,
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f
                )
            )

            val btnRechazar =
                Button(this@SolicitudesActivity).apply {
                    text = "Rechazar"
                    isAllCaps = false

                    setTextColor(
                        Color.parseColor("#C62828")
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    setBackgroundResource(
                        R.drawable.bg_button_outline_danger_evohka
                    )

                    backgroundTintList = null

                    setOnClickListener {
                        confirmarRespuesta(
                            solicitud,
                            "rechazada"
                        )
                    }
                }

            addView(
                btnRechazar,
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f
                ).apply {
                    marginStart = dp(10)
                }
            )
        }
    }

    private fun confirmarRespuesta(
        solicitud: Solicitud,
        estado: String
    ) {
        if (actualizandoEstado) {
            return
        }

        val accion =
            if (estado == "aprobada") {
                "aprobar"
            } else {
                "rechazar"
            }

        AlertDialog.Builder(this)
            .setTitle(
                if (estado == "aprobada") {
                    "Aprobar solicitud"
                } else {
                    "Rechazar solicitud"
                }
            )
            .setMessage(
                "¿Deseas $accion la solicitud para ${solicitud.titulo_propiedad ?: "este inmueble"}?"
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton("Confirmar") { _, _ ->
                actualizarEstado(
                    solicitud.id_solicitud,
                    estado
                )
            }
            .show()
    }

    private fun actualizarEstado(
        idSolicitud: Int,
        estado: String
    ) {
        val token =
            getSharedPreferences(
                "EvohkaSesion",
                MODE_PRIVATE
            ).getString(
                "token",
                null
            )

        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "La sesión ha finalizado. Inicia sesión nuevamente.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        actualizandoEstado = true
        txtEstadoSolicitudes.text =
            "Actualizando solicitud..."

        RetrofitClient.apiService
            .actualizarEstadoSolicitud(
                token,
                idSolicitud,
                ActualizarEstadoSolicitudRequest(
                    estado = estado
                )
            )
            .enqueue(
                object : Callback<RespuestaSolicitud> {

                    override fun onResponse(
                        call: Call<RespuestaSolicitud>,
                        response: Response<RespuestaSolicitud>
                    ) {
                        actualizandoEstado = false

                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@SolicitudesActivity,
                                response.body()?.mensaje
                                    ?: "Solicitud actualizada correctamente",
                                Toast.LENGTH_LONG
                            ).show()

                            cargarSolicitudes()
                        } else {
                            txtEstadoSolicitudes.text =
                                "No se pudo actualizar la solicitud"

                            Toast.makeText(
                                this@SolicitudesActivity,
                                obtenerMensajeError(
                                    response,
                                    "No se pudo actualizar la solicitud"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RespuestaSolicitud>,
                        t: Throwable
                    ) {
                        actualizandoEstado = false
                        txtEstadoSolicitudes.text =
                            "Error de conexión"

                        Toast.makeText(
                            this@SolicitudesActivity,
                            "No se pudo conectar con el servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    private fun etiquetaEstado(
        estado: String
    ): String {
        return when (estado) {
            "pendiente" -> "Pendiente"
            "aprobada" -> "Aprobada"
            "rechazada" -> "Rechazada"
            else -> "Sin estado"
        }
    }

    private fun colorEstado(
        estado: String
    ): Int {
        return when (estado) {
            "aprobada" ->
                Color.parseColor("#2E7D32")

            "rechazada" ->
                Color.parseColor("#C62828")

            else ->
                Color.parseColor("#7B2FF7")
        }
    }

    private fun obtenerMensajeError(
        response: Response<*>,
        mensajePredeterminado: String
    ): String {
        return try {
            val contenido =
                response.errorBody()
                    ?.string()

            if (contenido.isNullOrBlank()) {
                mensajePredeterminado
            } else {
                JSONObject(contenido)
                    .optString(
                        "mensaje",
                        mensajePredeterminado
                    )
            }
        } catch (error: Exception) {
            mensajePredeterminado
        }
    }
}

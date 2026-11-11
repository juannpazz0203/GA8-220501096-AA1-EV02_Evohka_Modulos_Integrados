package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale

class NotificacionesActivity : EvohkaActivity() {

    private lateinit var txtEstado: TextView
    private lateinit var contenedor: LinearLayout
    private var notificacionesActuales: List<Notificacion> = emptyList()
    private var noLeidasActuales = 0
    private val notificacionesEnActualizacion = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        txtEstado = findViewById(R.id.txtEstadoNotificaciones)
        contenedor = findViewById(R.id.contenedorNotificaciones)

        NavegacionEvohka.configurarSegunRol(
            this,
            R.id.contenedorNavegacionNotificaciones,
            NavegacionEvohka.DestinoPropietario.NOTIFICACIONES,
            NavegacionEvohka.DestinoArrendatario.NOTIFICACIONES
        )

    }

    override fun onResume() {
        super.onResume()
        cargarNotificaciones()
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density)
            .toInt()
    }

    private fun cargarNotificaciones() {
        val token = SesionSegura.obtenerToken()

        if (token.isNullOrBlank()) {
            txtEstado.text = "Sesión no válida"
            return
        }

        txtEstado.text = "Consultando notificaciones..."

        RetrofitClient.apiService
            .listarNotificaciones(token)
            .enqueue(
                object : Callback<RespuestaNotificaciones> {
                    override fun onResponse(
                        call: Call<RespuestaNotificaciones>,
                        response: Response<RespuestaNotificaciones>
                    ) {
                        if (response.isSuccessful) {
                            val cuerpo = response.body()

                            if (cuerpo == null) {
                                mostrarErrorProcesamiento()
                            } else {
                                actualizarEstadoNotificaciones(
                                    cuerpo.datos,
                                    cuerpo.no_leidas
                                )
                            }
                        } else {
                            txtEstado.text =
                                "Error del servidor (${response.code()})"

                            Toast.makeText(
                                this@NotificacionesActivity,
                                mensajeError(
                                    response,
                                    "No se pudieron consultar las notificaciones"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RespuestaNotificaciones>,
                        t: Throwable
                    ) {
                        mostrarFallo(t)
                    }
                }
            )
    }

    private fun mostrarNotificaciones(
        notificaciones: List<Notificacion>,
        noLeidas: Int
    ) {
        contenedor.removeAllViews()

        if (notificaciones.isEmpty()) {
            txtEstado.text = "No tienes notificaciones"
            return
        }

        txtEstado.text =
            "$noLeidas sin leer · ${notificaciones.size} en total"

        notificaciones.forEach { notificacion ->
            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
                setBackgroundResource(
                    if (notificacion.leida) {
                        R.drawable.bg_card_evohka
                    } else {
                        R.drawable.bg_notification_unread_evohka
                    }
                )
                elevation = dp(2).toFloat()
            }

            tarjeta.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }

            tarjeta.addView(
                texto(
                    notificacion.titulo ?: "Notificación",
                    17f,
                    true,
                    "#202020"
                )
            )

            tarjeta.addView(
                texto(
                    notificacion.mensaje ?: "Sin mensaje",
                    14f,
                    false,
                    "#444444",
                    dp(7)
                )
            )

            tarjeta.addView(
                texto(
                    FormatoEvohka.fechaHoraLocal(
                        notificacion.fecha
                    ),
                    13f,
                    false,
                    "#777777",
                    dp(8)
                )
            )

            val destino = destinoPara(notificacion)

            if (destino != null) {
                tarjeta.addView(
                    botonAccion(
                        "Ver",
                        true
                    ) {
                        if (notificacion.leida) {
                            abrirModulo(destino)
                        } else {
                            marcarLeida(
                                notificacion.id_notificacion
                            ) {
                                abrirModulo(destino)
                            }
                        }
                    },
                    parametrosBoton()
                )
            }

            if (!notificacion.leida) {
                tarjeta.addView(
                    botonAccion(
                        "Marcar como leída",
                        destino == null
                    ) {
                        marcarLeida(
                            notificacion.id_notificacion
                        )
                    },
                    parametrosBoton()
                )
            }

            contenedor.addView(tarjeta)
        }
    }

    private fun actualizarEstadoNotificaciones(
        notificaciones: List<Notificacion>,
        noLeidas: Int
    ) {
        notificacionesActuales = notificaciones
        noLeidasActuales = noLeidas.coerceAtLeast(0)
        mostrarNotificaciones(
            notificacionesActuales,
            noLeidasActuales
        )
        NavegacionEvohka.mostrarIndicadorNotificaciones(
            this,
            noLeidasActuales
        )
    }

    private fun actualizarNotificacionLeida(
        idNotificacion: Int
    ) {
        val estabaPendiente = notificacionesActuales.any {
            it.id_notificacion == idNotificacion && !it.leida
        }

        if (!estabaPendiente) return

        notificacionesActuales = notificacionesActuales.map {
            if (it.id_notificacion == idNotificacion) {
                it.copy(leida = true)
            } else {
                it
            }
        }
        noLeidasActuales = (noLeidasActuales - 1).coerceAtLeast(0)

        mostrarNotificaciones(
            notificacionesActuales,
            noLeidasActuales
        )
        NavegacionEvohka.mostrarIndicadorNotificaciones(
            this,
            noLeidasActuales
        )
    }

    private fun destinoPara(
        notificacion: Notificacion
    ): Class<out Activity>? {
        return when (
            notificacion.titulo
                ?.trim()
                ?.lowercase(Locale.ROOT)
        ) {
            "nuevo mensaje" ->
                ConversacionesActivity::class.java

            "nuevo contrato",
            "contrato actualizado",
            "tu contrato vence en 7 días",
            "tu contrato vence hoy" ->
                ContratosActivity::class.java

            "pago registrado",
            "tu pago vence en 3 días",
            "tu pago vence hoy" ->
                PagosActivity::class.java

            "nueva falla reportada",
            "falla creada",
            "falla actualizada",
            "estado de falla actualizado",
            "cambio de estado de falla" ->
                FallasActivity::class.java

            else -> null
        }
    }

    private fun abrirModulo(
        destino: Class<out Activity>
    ) {
        startActivity(
            Intent(this, destino).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }

    private fun botonAccion(
        etiqueta: String,
        principal: Boolean,
        alPulsar: () -> Unit
    ): Button {
        return Button(this).apply {
            text = etiqueta
            isAllCaps = false
            setTextColor(Color.parseColor("#170E0E"))
            setTypeface(null, Typeface.BOLD)
            setBackgroundResource(
                if (principal) {
                    R.drawable.bg_button_evohka
                } else {
                    R.drawable.bg_button_secondary_evohka
                }
            )
            backgroundTintList = null
            setOnClickListener { alPulsar() }
        }
    }

    private fun parametrosBoton(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(46)
        ).apply {
            topMargin = dp(10)
        }
    }

    private fun texto(
        contenido: String,
        tamano: Float,
        negrita: Boolean,
        color: String,
        margenSuperior: Int = 0
    ): TextView {
        return TextView(this).apply {
            text = contenido
            textSize = tamano
            setTextColor(Color.parseColor(color))

            if (negrita) {
                setTypeface(null, Typeface.BOLD)
            }

            if (margenSuperior > 0) {
                setPadding(0, margenSuperior, 0, 0)
            }
        }
    }

    private fun marcarLeida(
        idNotificacion: Int,
        alConfirmar: (() -> Unit)? = null
    ) {
        if (!notificacionesEnActualizacion.add(idNotificacion)) {
            return
        }

        val token = SesionSegura.obtenerToken()

        if (token.isNullOrBlank()) {
            notificacionesEnActualizacion.remove(idNotificacion)
            Toast.makeText(
                this,
                "La sesión no es válida",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        RetrofitClient.apiService
            .marcarNotificacionLeida(
                token,
                idNotificacion
            )
            .enqueue(
                object : Callback<RespuestaNotificacion> {
                    override fun onResponse(
                        call: Call<RespuestaNotificacion>,
                        response: Response<RespuestaNotificacion>
                    ) {
                        notificacionesEnActualizacion.remove(idNotificacion)

                        val confirmada = response.body()?.datos
                        if (
                            response.isSuccessful &&
                            confirmada?.id_notificacion == idNotificacion &&
                            confirmada.leida
                        ) {
                            actualizarNotificacionLeida(idNotificacion)
                            alConfirmar?.invoke()
                        } else {
                            Toast.makeText(
                                this@NotificacionesActivity,
                                mensajeError(
                                    response,
                                    "No se pudo confirmar la lectura de la notificación"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RespuestaNotificacion>,
                        t: Throwable
                    ) {
                        notificacionesEnActualizacion.remove(idNotificacion)
                        mostrarFallo(t)
                    }
                }
            )
    }

    private fun mostrarFallo(error: Throwable) {
        if (error is IOException) {
            txtEstado.text = "Error de conexión"

            Toast.makeText(
                this,
                "No se pudo conectar con el servidor",
                Toast.LENGTH_LONG
            ).show()
        } else {
            mostrarErrorProcesamiento()
        }
    }

    private fun mostrarErrorProcesamiento() {
        txtEstado.text =
            "No se pudo procesar la respuesta"

        Toast.makeText(
            this,
            "El servidor respondió, pero los datos recibidos no tienen el formato esperado",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun mensajeError(
        response: Response<*>,
        predeterminado: String
    ): String {
        return try {
            val contenido = response.errorBody()?.string()

            if (contenido.isNullOrBlank()) {
                predeterminado
            } else {
                JSONObject(contenido)
                    .optString("mensaje", predeterminado)
            }
        } catch (error: Exception) {
            predeterminado
        }
    }
}

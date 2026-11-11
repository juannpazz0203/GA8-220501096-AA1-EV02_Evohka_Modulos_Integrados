package com.example.evohkaapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecuperarContrasenaActivity : EvohkaActivity() {

    private var solicitudEnCurso = false
    private var llamadaActual: Call<RespuestaRecuperacion>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)
        aplicarInsetsSistema(incluirTeclado = true)

        val btnVolver =
            findViewById<ImageButton>(R.id.btnVolverRecuperacion)
        val etCorreo =
            findViewById<EditText>(R.id.etCorreoRecuperacion)
        val etConfirmarCorreo =
            findViewById<EditText>(R.id.etConfirmarCorreoRecuperacion)
        val btnSolicitar =
            findViewById<Button>(R.id.btnSolicitarCodigo)

        btnVolver.setOnClickListener {
            finish()
        }

        btnSolicitar.setOnClickListener {
            if (solicitudEnCurso) return@setOnClickListener

            val correo = etCorreo.text.toString().trim()
            val confirmacion = etConfirmarCorreo.text.toString().trim()

            if (correo.isEmpty() || confirmacion.isEmpty()) {
                mostrarMensaje("Ingresa y confirma tu correo electrónico")
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                mostrarMensaje("Ingresa un correo electrónico válido")
                return@setOnClickListener
            }

            if (!correo.equals(confirmacion, ignoreCase = true)) {
                mostrarMensaje("Los correos electrónicos no coinciden")
                return@setOnClickListener
            }

            cambiarEstadoSolicitud(btnSolicitar, enCurso = true)
            val llamada = RetrofitClient.apiService
                .solicitarRecuperacionContrasena(
                    SolicitarRecuperacionRequest(
                        correo = correo.lowercase()
                    )
                )
            llamadaActual = llamada
            llamada.enqueue(
                object : Callback<RespuestaRecuperacion> {
                    override fun onResponse(
                        call: Call<RespuestaRecuperacion>,
                        response: Response<RespuestaRecuperacion>
                    ) {
                        cambiarEstadoSolicitud(btnSolicitar, enCurso = false)

                        if (
                            response.isSuccessful &&
                            response.body()?.ok == true
                        ) {
                            mostrarMensaje(MENSAJE_NEUTRAL)
                            startActivity(
                                Intent(
                                    this@RecuperarContrasenaActivity,
                                    RestablecerContrasenaActivity::class.java
                                ).putExtra(
                                    EXTRA_CORREO,
                                    correo.lowercase()
                                )
                            )
                            return
                        }

                        val mensaje = when (response.code()) {
                            429 -> "Has realizado demasiadas solicitudes. Inténtalo más tarde."
                            503 -> "La recuperación no está disponible temporalmente."
                            else -> "No fue posible solicitar el código. Inténtalo nuevamente."
                        }
                        mostrarMensaje(mensaje)
                    }

                    override fun onFailure(
                        call: Call<RespuestaRecuperacion>,
                        error: Throwable
                    ) {
                        cambiarEstadoSolicitud(btnSolicitar, enCurso = false)
                        if (!call.isCanceled) {
                            mostrarMensaje("No se pudo conectar con el servidor")
                        }
                    }
                }
            )
        }
    }

    private fun cambiarEstadoSolicitud(
        boton: Button,
        enCurso: Boolean
    ) {
        solicitudEnCurso = enCurso
        boton.isEnabled = !enCurso
        boton.text = if (enCurso) "Enviando..." else "Continuar"
        boton.alpha = if (enCurso) 0.65f else 1f
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        llamadaActual?.cancel()
        llamadaActual = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CORREO = "correo_recuperacion"
        private const val MENSAJE_NEUTRAL =
            "Si el correo está registrado, recibirás un código de recuperación."
    }
}

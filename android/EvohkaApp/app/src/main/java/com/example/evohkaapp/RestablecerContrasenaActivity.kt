package com.example.evohkaapp

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RestablecerContrasenaActivity : EvohkaActivity() {

    private var restablecimientoEnCurso = false
    private var llamadaActual: Call<RespuestaRecuperacion>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restablecer_contrasena)
        aplicarInsetsSistema(incluirTeclado = true)

        val correo = intent.getStringExtra(
            RecuperarContrasenaActivity.EXTRA_CORREO
        )

        if (correo.isNullOrBlank()) {
            mostrarMensaje("Vuelve a solicitar un código de recuperación")
            finish()
            return
        }

        val btnVolver =
            findViewById<ImageButton>(R.id.btnVolverRestablecimiento)
        val etCodigo =
            findViewById<EditText>(R.id.etCodigoRecuperacion)
        val etNuevaContrasena =
            findViewById<EditText>(R.id.etNuevaContrasena)
        val etConfirmarContrasena =
            findViewById<EditText>(R.id.etConfirmarNuevaContrasena)
        val btnVerNuevaContrasena =
            findViewById<ImageButton>(R.id.btnVerNuevaContrasena)
        val btnVerConfirmacion =
            findViewById<ImageButton>(R.id.btnVerConfirmarNuevaContrasena)
        val btnRestablecer =
            findViewById<Button>(R.id.btnRestablecerContrasena)

        btnVolver.setOnClickListener {
            finish()
        }

        configurarVisibilidadContrasena(
            campo = etNuevaContrasena,
            boton = btnVerNuevaContrasena,
            descripcionVisible = "Ocultar nueva contraseña",
            descripcionOculta = "Mostrar nueva contraseña"
        )
        configurarVisibilidadContrasena(
            campo = etConfirmarContrasena,
            boton = btnVerConfirmacion,
            descripcionVisible = "Ocultar confirmación de contraseña",
            descripcionOculta = "Mostrar confirmación de contraseña"
        )

        btnRestablecer.setOnClickListener {
            if (restablecimientoEnCurso) return@setOnClickListener

            val codigo = etCodigo.text.toString()
            val nuevaContrasena = etNuevaContrasena.text.toString()
            val confirmacion = etConfirmarContrasena.text.toString()

            if (!codigo.matches(Regex("^\\d{8}$"))) {
                mostrarMensaje("Ingresa el código de 8 dígitos")
                return@setOnClickListener
            }

            if (nuevaContrasena.isEmpty() || confirmacion.isEmpty()) {
                mostrarMensaje("Ingresa y confirma la nueva contraseña")
                return@setOnClickListener
            }

            if (nuevaContrasena.length < 6) {
                mostrarMensaje("La contraseña debe tener mínimo 6 caracteres")
                return@setOnClickListener
            }

            if (nuevaContrasena.toByteArray(Charsets.UTF_8).size > 72) {
                mostrarMensaje("La contraseña no puede superar 72 bytes")
                return@setOnClickListener
            }

            if (nuevaContrasena != confirmacion) {
                mostrarMensaje("Las contraseñas no coinciden")
                return@setOnClickListener
            }

            cambiarEstadoRestablecimiento(btnRestablecer, enCurso = true)
            val llamada = RetrofitClient.apiService.restablecerContrasena(
                RestablecerContrasenaRequest(
                    correo = correo,
                    codigo = codigo,
                    nuevaContrasena = nuevaContrasena
                )
            )
            llamadaActual = llamada
            llamada.enqueue(
                object : Callback<RespuestaRecuperacion> {
                    override fun onResponse(
                        call: Call<RespuestaRecuperacion>,
                        response: Response<RespuestaRecuperacion>
                    ) {
                        cambiarEstadoRestablecimiento(
                            btnRestablecer,
                            enCurso = false
                        )

                        if (
                            response.isSuccessful &&
                            response.body()?.ok == true
                        ) {
                            completarRestablecimiento()
                            return
                        }

                        val mensaje = when (response.code()) {
                            400, 404 -> "El código es incorrecto o venció."
                            429 -> "Demasiados intentos. Inténtalo más tarde."
                            503 -> "La recuperación no está disponible temporalmente."
                            else -> "No fue posible restablecer la contraseña."
                        }
                        mostrarMensaje(mensaje)
                    }

                    override fun onFailure(
                        call: Call<RespuestaRecuperacion>,
                        error: Throwable
                    ) {
                        cambiarEstadoRestablecimiento(
                            btnRestablecer,
                            enCurso = false
                        )
                        if (!call.isCanceled) {
                            mostrarMensaje("No se pudo conectar con el servidor")
                        }
                    }
                }
            )
        }
    }

    private fun configurarVisibilidadContrasena(
        campo: EditText,
        boton: ImageButton,
        descripcionVisible: String,
        descripcionOculta: String
    ) {
        var visible = false
        boton.setOnClickListener {
            visible = !visible
            campo.transformationMethod = if (visible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            boton.setImageResource(
                if (visible) {
                    R.drawable.ic_visibility_off
                } else {
                    R.drawable.ic_visibility
                }
            )
            boton.contentDescription =
                if (visible) descripcionVisible else descripcionOculta
            campo.setSelection(campo.text.length)
        }
    }

    private fun cambiarEstadoRestablecimiento(
        boton: Button,
        enCurso: Boolean
    ) {
        restablecimientoEnCurso = enCurso
        boton.isEnabled = !enCurso
        boton.text = if (enCurso) "Verificando..." else "Restablecer contraseña"
        boton.alpha = if (enCurso) 0.65f else 1f
    }

    private fun completarRestablecimiento() {
        SesionSegura.cerrarSesion()
        NavegacionSesionInvalida.reiniciar()

        mostrarMensaje("Contraseña restablecida. Inicia sesión nuevamente.")
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        llamadaActual?.cancel()
        llamadaActual = null
        super.onDestroy()
    }
}

package com.example.evohkaapp

import android.app.Activity
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistroActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val btnVolver =
            findViewById<TextView>(R.id.btnVolver)

        val etNombre =
            findViewById<EditText>(R.id.etNombre)

        val etApellido =
            findViewById<EditText>(R.id.etApellido)

        val etCorreo =
            findViewById<EditText>(R.id.etCorreo)

        val etTelefono =
            findViewById<EditText>(R.id.etTelefono)

        val etContrasena =
            findViewById<EditText>(R.id.etContrasena)

        val etConfirmarContrasena =
            findViewById<EditText>(R.id.etConfirmarContrasena)

        val btnVerContrasena =
            findViewById<ImageButton>(R.id.btnVerContrasena)

        val btnVerConfirmacion =
            findViewById<ImageButton>(R.id.btnVerConfirmacion)

        val rgRol =
            findViewById<RadioGroup>(R.id.rgRol)

        val btnConfirmarRegistro =
            findViewById<Button>(R.id.btnConfirmarRegistro)

        var contrasenaVisible = false
        var confirmacionVisible = false

        btnVolver.setOnClickListener {
            finish()
        }

        btnVerContrasena.setOnClickListener {

            contrasenaVisible = !contrasenaVisible

            if (contrasenaVisible) {

                etContrasena.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()

                btnVerContrasena.setImageResource(
                    R.drawable.ic_visibility_off
                )

                btnVerContrasena.contentDescription =
                    "Ocultar contraseña"

            } else {

                etContrasena.transformationMethod =
                    PasswordTransformationMethod.getInstance()

                btnVerContrasena.setImageResource(
                    R.drawable.ic_visibility
                )

                btnVerContrasena.contentDescription =
                    "Mostrar contraseña"
            }

            etContrasena.setSelection(
                etContrasena.text.length
            )
        }

        btnVerConfirmacion.setOnClickListener {

            confirmacionVisible = !confirmacionVisible

            if (confirmacionVisible) {

                etConfirmarContrasena.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()

                btnVerConfirmacion.setImageResource(
                    R.drawable.ic_visibility_off
                )

                btnVerConfirmacion.contentDescription =
                    "Ocultar confirmación"

            } else {

                etConfirmarContrasena.transformationMethod =
                    PasswordTransformationMethod.getInstance()

                btnVerConfirmacion.setImageResource(
                    R.drawable.ic_visibility
                )

                btnVerConfirmacion.contentDescription =
                    "Mostrar confirmación"
            }

            etConfirmarContrasena.setSelection(
                etConfirmarContrasena.text.length
            )
        }

        btnConfirmarRegistro.setOnClickListener {

            val nombre =
                etNombre.text.toString().trim()

            val apellido =
                etApellido.text.toString().trim()

            val correo =
                etCorreo.text.toString().trim()

            val telefono =
                etTelefono.text.toString().trim()

            val contrasena =
                etContrasena.text.toString()

            val confirmarContrasena =
                etConfirmarContrasena.text.toString()

            if (
                nombre.isEmpty() ||
                apellido.isEmpty() ||
                correo.isEmpty() ||
                contrasena.isEmpty() ||
                confirmarContrasena.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Completa los campos obligatorios",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (
                !Patterns.EMAIL_ADDRESS
                    .matcher(correo)
                    .matches()
            ) {

                Toast.makeText(
                    this,
                    "Ingresa un correo válido",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (contrasena.length < 6) {

                Toast.makeText(
                    this,
                    "La contraseña debe tener mínimo 6 caracteres",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (
                contrasena != confirmarContrasena
            ) {

                Toast.makeText(
                    this,
                    "Las contraseñas no coinciden",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val rol =
                when (
                    rgRol.checkedRadioButtonId
                ) {

                    R.id.rbInquilino ->
                        "inquilino"

                    R.id.rbPropietario ->
                        "propietario"

                    else ->
                        null
                }

            if (rol == null) {

                Toast.makeText(
                    this,
                    "Selecciona si buscas alquilar o quieres publicar",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            btnConfirmarRegistro.isEnabled =
                false

            val datosRegistro =
                RegistroRequest(
                    nombre = nombre,
                    apellido = apellido,
                    correo = correo,
                    contrasena = contrasena,
                    telefono =
                        telefono.ifEmpty {
                            null
                        },
                    rol = rol
                )

            RetrofitClient.apiService
                .registrarUsuario(
                    datosRegistro
                )
                .enqueue(
                    object :
                        Callback<RegistroResponse> {

                        override fun onResponse(
                            call:
                                Call<RegistroResponse>,
                            response:
                                Response<RegistroResponse>
                        ) {

                            btnConfirmarRegistro
                                .isEnabled = true

                            if (
                                response.isSuccessful
                            ) {

                                val respuesta =
                                    response.body()

                                Toast.makeText(
                                    this@RegistroActivity,
                                    respuesta?.mensaje
                                        ?: "Usuario registrado correctamente",
                                    Toast.LENGTH_LONG
                                ).show()

                                finish()
                                return
                            }

                            val mensajeError =
                                try {

                                    val contenido =
                                        response
                                            .errorBody()
                                            ?.string()

                                    if (
                                        !contenido
                                            .isNullOrEmpty()
                                    ) {

                                        JSONObject(
                                            contenido
                                        ).optString(
                                            "mensaje",
                                            "No fue posible realizar el registro"
                                        )

                                    } else {

                                        "No fue posible realizar el registro"
                                    }

                                } catch (
                                    e: Exception
                                ) {

                                    "No fue posible realizar el registro"
                                }

                            Toast.makeText(
                                this@RegistroActivity,
                                mensajeError,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onFailure(
                            call:
                                Call<RegistroResponse>,
                            t:
                                Throwable
                        ) {

                            btnConfirmarRegistro
                                .isEnabled = true

                            Toast.makeText(
                                this@RegistroActivity,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
        }
    }
}
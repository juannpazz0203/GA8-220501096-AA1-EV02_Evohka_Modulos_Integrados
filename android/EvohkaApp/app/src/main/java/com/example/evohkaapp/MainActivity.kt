package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (redirigirSiHaySesion()) {
            return
        }

        setContentView(R.layout.activity_login)

        val correo = findViewById<EditText>(R.id.correo)
        val password = findViewById<EditText>(R.id.password)

        val btnVerPassword =
            findViewById<ImageButton>(R.id.btnVerPassword)

        val grupoTipoIngreso =
            findViewById<RadioGroup>(R.id.grupo_tipo_ingreso)

        val btnContinuar =
            findViewById<Button>(R.id.btnContinuar)

        val txtRegistrarse =
            findViewById<TextView>(R.id.txtRegistrarse)

        var passwordVisible = false

        btnVerPassword.setOnClickListener {

            passwordVisible = !passwordVisible

            if (passwordVisible) {

                password.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()

                btnVerPassword.setImageResource(
                    R.drawable.ic_visibility_off
                )

                btnVerPassword.contentDescription =
                    "Ocultar contraseña"

            } else {

                password.transformationMethod =
                    PasswordTransformationMethod.getInstance()

                btnVerPassword.setImageResource(
                    R.drawable.ic_visibility
                )

                btnVerPassword.contentDescription =
                    "Mostrar contraseña"
            }

            password.setSelection(password.text.length)
        }

        txtRegistrarse.setOnClickListener {

            val intent = Intent(
                this,
                RegistroActivity::class.java
            )

            startActivity(intent)
        }

        btnContinuar.setOnClickListener {

            val correoIngresado =
                correo.text.toString().trim()

            val contrasenaIngresada =
                password.text.toString().trim()

            if (
                correoIngresado.isEmpty() ||
                contrasenaIngresada.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Ingrese correo y contraseña",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val opcionSeleccionada =
                grupoTipoIngreso.checkedRadioButtonId

            if (opcionSeleccionada == -1) {

                Toast.makeText(
                    this,
                    "Seleccione si desea ingresar como inquilino o propietario",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val radioSeleccionado =
                findViewById<RadioButton>(
                    opcionSeleccionada
                )

            val tipoIngresoSeleccionado =
                radioSeleccionado.text
                    .toString()
                    .lowercase()

            val datosLogin =
                LoginRequest(
                    correoIngresado,
                    contrasenaIngresada
                )

            RetrofitClient.apiService
                .login(datosLogin)
                .enqueue(
                    object : Callback<LoginResponse> {

                        override fun onResponse(
                            call: Call<LoginResponse>,
                            response: Response<LoginResponse>
                        ) {

                            if (
                                response.isSuccessful &&
                                response.body()?.ok == true
                            ) {

                                val respuesta =
                                    response.body()

                                val usuario =
                                    respuesta?.usuario

                                if (usuario == null) {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "No se pudo identificar el usuario",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return
                                }

                                val rolReal =
                                    usuario.rol
                                        .trim()
                                        .lowercase()

                                val rolSeleccionado =
                                    if (
                                        tipoIngresoSeleccionado == "inquilino"
                                    ) {
                                        "arrendatario"
                                    } else {
                                        "propietario"
                                    }

                                if (rolReal != rolSeleccionado) {

                                    val mensajeRol =
                                        if (rolReal == "arrendatario") {
                                            "Este usuario está registrado como inquilino"
                                        } else if (
                                            rolReal == "propietario"
                                        ) {
                                            "Este usuario está registrado como propietario"
                                        } else {
                                            "El rol de este usuario no está habilitado"
                                        }

                                    Toast.makeText(
                                        this@MainActivity,
                                        mensajeRol,
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return
                                }

                                val token =
                                    respuesta.token

                                if (token.isNullOrBlank()) {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "No se pudo iniciar una sesión válida",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return
                                }

                                val preferencias =
                                    getSharedPreferences(
                                        "EvohkaSesion",
                                        MODE_PRIVATE
                                    )

                                preferencias.edit()
                                    .putString(
                                        "token",
                                        token
                                    )
                                    .putInt(
                                        "id_usuario",
                                        usuario.id_usuario
                                    )
                                    .putString(
                                        "nombre",
                                        usuario.nombre
                                    )
                                    .putString(
                                        "rol",
                                        rolReal
                                    )
                                    .putString(
                                        "tipo_ingreso",
                                        tipoIngresoSeleccionado
                                    )
                                    .apply()

                                Toast.makeText(
                                    this@MainActivity,
                                    "Bienvenido ${usuario.nombre}",
                                    Toast.LENGTH_LONG
                                ).show()

                                abrirPantallaPrincipal(
                                    rolReal
                                )

                            } else {

                                Toast.makeText(
                                    this@MainActivity,
                                    "Correo o contraseña incorrectos",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<LoginResponse>,
                            t: Throwable
                        ) {

                            Toast.makeText(
                                this@MainActivity,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
        }
    }

    private fun redirigirSiHaySesion(): Boolean {

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

        val idUsuario =
            preferencias.getInt(
                "id_usuario",
                0
            )

        val rol =
            preferencias.getString(
                "rol",
                null
            )

        if (
            token.isNullOrBlank() ||
            idUsuario <= 0 ||
            rol.isNullOrBlank()
        ) {
            return false
        }

        return abrirPantallaPrincipal(
            rol
        )
    }

    private fun abrirPantallaPrincipal(
        rol: String
    ): Boolean {

        val intent =
            when (
                rol.trim().lowercase()
            ) {

                "propietario" ->
                    Intent(
                        this,
                        InicioPropietarioActivity::class.java
                    )

                "arrendatario" ->
                    Intent(
                        this,
                        PropiedadesDisponiblesActivity::class.java
                    )

                else ->
                    return false
            }

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()

        return true
    }
}

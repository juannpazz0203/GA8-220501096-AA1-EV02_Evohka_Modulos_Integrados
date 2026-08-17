package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val correo = findViewById<EditText>(R.id.correo)
        val password = findViewById<EditText>(R.id.password)
        val btnContinuar = findViewById<Button>(R.id.btnContinuar)
        val txtRegistrarse = findViewById<TextView>(R.id.txtRegistrarse)

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

            val datosLogin = LoginRequest(
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

                                Toast.makeText(
                                    this@MainActivity,
                                    "Bienvenido ${respuesta?.usuario?.nombre}",
                                    Toast.LENGTH_LONG
                                ).show()

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
}
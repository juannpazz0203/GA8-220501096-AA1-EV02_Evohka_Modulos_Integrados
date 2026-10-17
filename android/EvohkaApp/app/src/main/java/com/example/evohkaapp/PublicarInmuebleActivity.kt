package com.example.evohkaapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PublicarInmuebleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_publicar_inmueble)

        val etTitulo =
            findViewById<EditText>(R.id.et_titulo)

        val etCiudad =
            findViewById<EditText>(R.id.et_ciudad)

        val etDireccion =
            findViewById<EditText>(R.id.et_direccion)

        val etPrecioMensual =
            findViewById<EditText>(R.id.et_precio_mensual)

        val btnGuardarInmueble =
            findViewById<Button>(R.id.btn_guardar_inmueble)

        val btnCancelar =
            findViewById<Button>(R.id.btn_cancelar)

        btnCancelar.setOnClickListener {
            finish()
        }

        btnGuardarInmueble.setOnClickListener {

            val titulo =
                etTitulo.text.toString().trim()

            val ciudad =
                etCiudad.text.toString().trim()

            val direccion =
                etDireccion.text.toString().trim()

            val precioTexto =
                etPrecioMensual.text.toString().trim()

            if (
                titulo.isEmpty() ||
                ciudad.isEmpty() ||
                direccion.isEmpty() ||
                precioTexto.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val precioMensual =
                precioTexto.toDoubleOrNull()

            if (
                precioMensual == null ||
                precioMensual <= 0
            ) {

                Toast.makeText(
                    this,
                    "Ingresa un canon mensual válido",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val nuevaPropiedad =
                PropiedadRequest(
                    titulo = titulo,
                    ciudad = ciudad,
                    direccion = direccion,
                    precio_mensual = precioMensual
                )

            btnGuardarInmueble.isEnabled = false
            btnGuardarInmueble.text = "Publicando..."

            RetrofitClient.apiService
                .registrarPropiedad(nuevaPropiedad)
                .enqueue(
                    object : Callback<RespuestaPropiedad> {

                        override fun onResponse(
                            call: Call<RespuestaPropiedad>,
                            response: Response<RespuestaPropiedad>
                        ) {

                            btnGuardarInmueble.isEnabled = true
                            btnGuardarInmueble.text =
                                "Publicar inmueble"

                            if (response.isSuccessful) {

                                Toast.makeText(
                                    this@PublicarInmuebleActivity,
                                    "Propiedad publicada correctamente",
                                    Toast.LENGTH_LONG
                                ).show()

                                finish()

                            } else {

                                Toast.makeText(
                                    this@PublicarInmuebleActivity,
                                    "No se pudo registrar la propiedad",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<RespuestaPropiedad>,
                            t: Throwable
                        ) {

                            btnGuardarInmueble.isEnabled = true
                            btnGuardarInmueble.text =
                                "Publicar inmueble"

                            Toast.makeText(
                                this@PublicarInmuebleActivity,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
        }
    }
}
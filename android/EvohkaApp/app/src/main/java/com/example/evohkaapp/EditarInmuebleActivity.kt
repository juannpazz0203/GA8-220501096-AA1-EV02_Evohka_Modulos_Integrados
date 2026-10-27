package com.example.evohkaapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditarInmuebleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_editar_inmueble)

        val idPropiedad =
            intent.getIntExtra(
                "id_propiedad",
                0
            )

        if (idPropiedad <= 0) {
            Toast.makeText(
                this,
                "No se pudo identificar la propiedad",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val etTitulo =
            findViewById<EditText>(R.id.et_titulo)

        val etTipoInmueble =
            findViewById<EditText>(R.id.et_tipo_inmueble)

        val etDireccion =
            findViewById<EditText>(R.id.et_direccion)

        val etBarrio =
            findViewById<EditText>(R.id.et_barrio)

        val etCiudad =
            findViewById<EditText>(R.id.et_ciudad)

        val etCanon =
            findViewById<EditText>(R.id.et_canon)

        val etHabitaciones =
            findViewById<EditText>(R.id.et_habitaciones)

        val etBanos =
            findViewById<EditText>(R.id.et_banos)

        val btnActualizar =
            findViewById<Button>(R.id.btn_actualizar_inmueble)

        val btnCancelar =
            findViewById<Button>(R.id.btn_cancelar_edicion)

        etTitulo.setText(
            intent.getStringExtra("titulo").orEmpty()
        )

        etTipoInmueble.setText(
            intent.getStringExtra("tipo_inmueble").orEmpty()
        )

        etDireccion.setText(
            intent.getStringExtra("direccion").orEmpty()
        )

        etBarrio.setText(
            intent.getStringExtra("barrio").orEmpty()
        )

        etCiudad.setText(
            intent.getStringExtra("ciudad").orEmpty()
        )

        etCanon.setText(
            intent.getDoubleExtra(
                "canon",
                0.0
            ).toString()
        )

        etHabitaciones.setText(
            intent.getIntExtra(
                "habitaciones",
                0
            ).toString()
        )

        etBanos.setText(
            intent.getIntExtra(
                "banos",
                0
            ).toString()
        )

        btnCancelar.setOnClickListener {
            finish()
        }

        btnActualizar.setOnClickListener {

            val titulo =
                etTitulo.text.toString().trim()

            val tipoInmueble =
                etTipoInmueble.text.toString().trim()

            val direccion =
                etDireccion.text.toString().trim()

            val barrio =
                etBarrio.text.toString().trim()

            val ciudad =
                etCiudad.text.toString().trim()

            val canonTexto =
                etCanon.text.toString().trim()

            val habitacionesTexto =
                etHabitaciones.text.toString().trim()

            val banosTexto =
                etBanos.text.toString().trim()

            if (
                titulo.isEmpty() ||
                tipoInmueble.isEmpty() ||
                direccion.isEmpty() ||
                barrio.isEmpty() ||
                ciudad.isEmpty() ||
                canonTexto.isEmpty() ||
                habitacionesTexto.isEmpty() ||
                banosTexto.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val canon =
                canonTexto.toDoubleOrNull()

            val habitaciones =
                habitacionesTexto.toIntOrNull()

            val banos =
                banosTexto.toIntOrNull()

            if (canon == null || canon <= 0) {
                Toast.makeText(
                    this,
                    "Ingresa un canon mensual válido",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (habitaciones == null || habitaciones < 0) {
                Toast.makeText(
                    this,
                    "Ingresa una cantidad válida de habitaciones",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (banos == null || banos < 0) {
                Toast.makeText(
                    this,
                    "Ingresa una cantidad válida de baños",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

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

                return@setOnClickListener
            }

            val propiedadActualizada =
                PropiedadRequest(
                    titulo = titulo,
                    tipo_inmueble = tipoInmueble,
                    direccion = direccion,
                    barrio = barrio,
                    ciudad = ciudad,
                    canon = canon,
                    habitaciones = habitaciones,
                    banos = banos
                )

            btnActualizar.isEnabled = false
            btnActualizar.text = "Guardando..."

            RetrofitClient.apiService
                .actualizarPropiedad(
                    token,
                    idPropiedad,
                    propiedadActualizada
                )
                .enqueue(
                    object : Callback<RespuestaPropiedad> {

                        override fun onResponse(
                            call: Call<RespuestaPropiedad>,
                            response: Response<RespuestaPropiedad>
                        ) {
                            btnActualizar.isEnabled = true
                            btnActualizar.text = "Guardar cambios"

                            if (response.isSuccessful) {
                                Toast.makeText(
                                    this@EditarInmuebleActivity,
                                    response.body()?.mensaje
                                        ?: "Propiedad actualizada correctamente",
                                    Toast.LENGTH_LONG
                                ).show()

                                finish()
                            } else {
                                Toast.makeText(
                                    this@EditarInmuebleActivity,
                                    obtenerMensajeError(
                                        response,
                                        "No se pudo actualizar la propiedad"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<RespuestaPropiedad>,
                            t: Throwable
                        ) {
                            btnActualizar.isEnabled = true
                            btnActualizar.text = "Guardar cambios"

                            Toast.makeText(
                                this@EditarInmuebleActivity,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
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

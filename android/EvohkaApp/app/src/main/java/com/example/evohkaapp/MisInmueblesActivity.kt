package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class MisInmueblesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mis_inmuebles)

        val btnVolverInicio =
            findViewById<Button>(R.id.btnVolverInicio)

        val btnPublicarNuevo =
            findViewById<Button>(R.id.btnPublicarNuevo)

        btnVolverInicio.setOnClickListener {
            finish()
        }

        btnPublicarNuevo.setOnClickListener {
            val intent = Intent(
                this,
                PublicarInmuebleActivity::class.java
            )

            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        val txtEstadoCarga =
            findViewById<TextView>(R.id.txtEstadoCarga)

        val contenedorInmuebles =
            findViewById<LinearLayout>(R.id.contenedorInmuebles)

        cargarInmuebles(
            txtEstadoCarga,
            contenedorInmuebles
        )
    }

    private fun cargarInmuebles(
        txtEstadoCarga: TextView,
        contenedorInmuebles: LinearLayout
    ) {

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

        if (token.isNullOrBlank() || idUsuario == 0) {

            txtEstadoCarga.text =
                "Sesión no válida"

            Toast.makeText(
                this,
                "Debe iniciar sesión nuevamente",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        txtEstadoCarga.text =
            "Consultando inmuebles..."

        RetrofitClient.apiService
            .listarPropiedades(token)
            .enqueue(
                object : Callback<RespuestaPropiedades> {

                    override fun onResponse(
                        call: Call<RespuestaPropiedades>,
                        response: Response<RespuestaPropiedades>
                    ) {

                        if (response.isSuccessful) {

                            val propiedades =
                                response.body()?.datos
                                    ?: emptyList()

                            val misPropiedades =
                                propiedades.filter {
                                    it.id_propietario == idUsuario
                                }

                            mostrarPropiedades(
                                misPropiedades,
                                txtEstadoCarga,
                                contenedorInmuebles
                            )

                        } else {

                            txtEstadoCarga.text =
                                "No se pudieron consultar los inmuebles"

                            Toast.makeText(
                                this@MisInmueblesActivity,
                                "Error del servidor: ${response.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RespuestaPropiedades>,
                        t: Throwable
                    ) {

                        txtEstadoCarga.text =
                            "Error de conexión"

                        Toast.makeText(
                            this@MisInmueblesActivity,
                            "No se pudo conectar con el servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    private fun mostrarPropiedades(
        propiedades: List<Propiedad>,
        txtEstadoCarga: TextView,
        contenedorInmuebles: LinearLayout
    ) {

        contenedorInmuebles.removeAllViews()

        if (propiedades.isEmpty()) {

            txtEstadoCarga.text =
                "No tienes inmuebles registrados"

            return
        }

        txtEstadoCarga.text =
            "Mis inmuebles: ${propiedades.size}"

        val formatoMoneda =
            NumberFormat.getCurrencyInstance(
                Locale("es", "CO")
            )

        propiedades.forEach { propiedad ->

            val tarjeta =
                TextView(this)

            val canon =
                formatoMoneda.format(
                    propiedad.canon ?: 0
                )

            tarjeta.text = """
                ${propiedad.titulo ?: "Inmueble"}

                Tipo: ${propiedad.tipo_inmueble ?: "No definido"}
                Dirección: ${propiedad.direccion ?: "Sin dirección"}
                Barrio: ${propiedad.barrio ?: "Sin barrio"}
                Ciudad: ${propiedad.ciudad ?: "Sin ciudad"}
                Canon mensual: $canon
                Habitaciones: ${propiedad.habitaciones ?: 0}
                Baños: ${propiedad.banos ?: 0}
            """.trimIndent()

            tarjeta.textSize = 16f

            tarjeta.setPadding(
                30,
                30,
                30,
                30
            )

            val parametros =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            parametros.setMargins(
                0,
                0,
                0,
                25
            )

            tarjeta.layoutParams =
                parametros

            contenedorInmuebles.addView(
                tarjeta
            )
        }
    }
}
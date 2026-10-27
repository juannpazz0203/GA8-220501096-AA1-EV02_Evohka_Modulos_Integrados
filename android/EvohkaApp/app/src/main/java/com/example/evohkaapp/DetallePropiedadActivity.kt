package com.example.evohkaapp

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class DetallePropiedadActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_detalle_propiedad)

        val txtTitulo =
            findViewById<TextView>(R.id.txt_titulo_detalle)

        val txtTipo =
            findViewById<TextView>(R.id.txt_tipo_detalle)

        val txtCiudad =
            findViewById<TextView>(R.id.txt_ciudad_detalle)

        val txtBarrio =
            findViewById<TextView>(R.id.txt_barrio_detalle)

        val txtDireccion =
            findViewById<TextView>(R.id.txt_direccion_detalle)

        val txtCanon =
            findViewById<TextView>(R.id.txt_canon_detalle)

        val txtHabitaciones =
            findViewById<TextView>(R.id.txt_habitaciones_detalle)

        val txtBanos =
            findViewById<TextView>(R.id.txt_banos_detalle)

        val btnSolicitarArriendo =
            findViewById<Button>(R.id.btn_solicitar_arriendo)

        val btnVolver =
            findViewById<Button>(R.id.btn_volver_detalle)

        val idPropiedad =
            intent.getIntExtra(
                "id_propiedad",
                0
            )

        val titulo =
            intent.getStringExtra("titulo")
                ?: "Inmueble"

        val tipoInmueble =
            intent.getStringExtra("tipo_inmueble")
                ?: "No definido"

        val direccion =
            intent.getStringExtra("direccion")
                ?: "Sin dirección"

        val barrio =
            intent.getStringExtra("barrio")
                ?: "Sin barrio"

        val ciudad =
            intent.getStringExtra("ciudad")
                ?: "Sin ciudad"

        val canon =
            intent.getDoubleExtra(
                "canon",
                0.0
            )

        val habitaciones =
            intent.getIntExtra(
                "habitaciones",
                0
            )

        val banos =
            intent.getIntExtra(
                "banos",
                0
            )

        val formatoMoneda =
            NumberFormat.getCurrencyInstance(
                Locale.forLanguageTag("es-CO")
            )

        txtTitulo.text =
            titulo

        txtTipo.text =
            "Tipo: $tipoInmueble"

        txtCiudad.text =
            "Ciudad: $ciudad"

        txtBarrio.text =
            "Barrio: $barrio"

        txtDireccion.text =
            "Dirección: $direccion"

        txtCanon.text =
            "Canon mensual: ${formatoMoneda.format(canon)}"

        txtHabitaciones.text =
            "Habitaciones: $habitaciones"

        txtBanos.text =
            "Baños: $banos"

        btnVolver.setOnClickListener {
            finish()
        }

        btnSolicitarArriendo.setOnClickListener {
            confirmarSolicitud(
                idPropiedad,
                titulo,
                btnSolicitarArriendo
            )
        }
    }

    private fun confirmarSolicitud(
        idPropiedad: Int,
        tituloPropiedad: String,
        btnSolicitarArriendo: Button
    ) {
        if (idPropiedad <= 0) {
            Toast.makeText(
                this,
                "No se pudo identificar la propiedad",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val preferencias =
            getSharedPreferences(
                "EvohkaSesion",
                MODE_PRIVATE
            )

        val rol =
            preferencias.getString(
                "rol",
                null
            )

        if (rol != "arrendatario") {
            Toast.makeText(
                this,
                "Solo los inquilinos pueden solicitar un arriendo",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle("Solicitar arriendo")
            .setMessage(
                "¿Deseas enviar una solicitud para $tituloPropiedad?"
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton("Enviar solicitud") { _, _ ->
                enviarSolicitud(
                    idPropiedad,
                    btnSolicitarArriendo
                )
            }
            .show()
    }

    private fun enviarSolicitud(
        idPropiedad: Int,
        btnSolicitarArriendo: Button
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

        btnSolicitarArriendo.isEnabled = false
        btnSolicitarArriendo.text = "Enviando solicitud..."

        RetrofitClient.apiService
            .crearSolicitud(
                token,
                CrearSolicitudRequest(
                    id_propiedad = idPropiedad
                )
            )
            .enqueue(
                object : Callback<RespuestaSolicitud> {

                    override fun onResponse(
                        call: Call<RespuestaSolicitud>,
                        response: Response<RespuestaSolicitud>
                    ) {
                        if (response.isSuccessful) {
                            btnSolicitarArriendo.text =
                                "Solicitud enviada"

                            Toast.makeText(
                                this@DetallePropiedadActivity,
                                response.body()?.mensaje
                                    ?: "Solicitud registrada correctamente",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            btnSolicitarArriendo.isEnabled = true
                            btnSolicitarArriendo.text =
                                "Solicitar arriendo"

                            Toast.makeText(
                                this@DetallePropiedadActivity,
                                obtenerMensajeError(
                                    response,
                                    "No se pudo registrar la solicitud"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RespuestaSolicitud>,
                        t: Throwable
                    ) {
                        btnSolicitarArriendo.isEnabled = true
                        btnSolicitarArriendo.text =
                            "Solicitar arriendo"

                        Toast.makeText(
                            this@DetallePropiedadActivity,
                            "No se pudo conectar con el servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
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

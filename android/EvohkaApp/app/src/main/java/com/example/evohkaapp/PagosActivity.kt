package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PagosActivity : EvohkaActivity() {

    private lateinit var txtEstado: TextView
    private lateinit var contenedor: LinearLayout
    private var idContrato: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagos)

        txtEstado = findViewById(R.id.txtEstadoPagos)
        contenedor = findViewById(R.id.contenedorPagos)

        val idRecibido = intent.getIntExtra(
            "id_contrato",
            0
        )

        idContrato = idRecibido
            .takeIf { it > 0 }

        val btnRegistrar =
            findViewById<Button>(R.id.btnRegistrarPago)

        val contratoActivo = intent.getStringExtra(
            "estado_contrato"
        )?.equals("activo", ignoreCase = true) == true

        if (idContrato != null && contratoActivo) {
            btnRegistrar.visibility = View.VISIBLE
            btnRegistrar.setOnClickListener {
                abrirRegistroPago()
            }
        }

        NavegacionEvohka.configurarSegunRol(
            this,
            R.id.contenedorNavegacionPagos,
            null,
            NavegacionEvohka.DestinoArrendatario.PAGOS
        )
    }

    override fun onResume() {
        super.onResume()
        cargarPagos()
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density)
            .toInt()
    }

    private fun cargarPagos() {
        val token = getSharedPreferences(
            "EvohkaSesion",
            MODE_PRIVATE
        ).getString("token", null)

        if (token.isNullOrBlank()) {
            txtEstado.text = "Sesión no válida"
            return
        }

        txtEstado.text = "Consultando pagos..."

        RetrofitClient.apiService.listarPagos(
            token,
            idContrato
        ).enqueue(
            object : Callback<RespuestaPagos> {
                override fun onResponse(
                    call: Call<RespuestaPagos>,
                    response: Response<RespuestaPagos>
                ) {
                    if (response.isSuccessful) {
                        mostrarPagos(
                            response.body()?.datos
                                ?: emptyList()
                        )
                    } else {
                        txtEstado.text =
                            "No se pudieron consultar los pagos"

                        Toast.makeText(
                            this@PagosActivity,
                            mensajeError(
                                response,
                                "No se pudieron consultar los pagos"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaPagos>,
                    t: Throwable
                ) {
                    txtEstado.text = "Error de conexión"

                    Toast.makeText(
                        this@PagosActivity,
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun mostrarPagos(pagos: List<Pago>) {
        contenedor.removeAllViews()

        if (pagos.isEmpty()) {
            txtEstado.text = "No hay pagos registrados"
            return
        }

        txtEstado.text =
            "${pagos.size} pago(s) registrado(s)"

        pagos.forEach { pago ->
            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
                setBackgroundResource(R.drawable.bg_card_evohka)
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
                    pago.titulo_propiedad ?: "Inmueble",
                    17f,
                    true,
                    "#202020"
                )
            )

            tarjeta.addView(
                texto(
                    FormatoEvohka.moneda(pago.monto),
                    20f,
                    true,
                    "#7B2FF7",
                    dp(8)
                )
            )

            tarjeta.addView(
                texto(
                    "Fecha: ${FormatoEvohka.fecha(pago.fecha_pago)}",
                    14f,
                    false,
                    "#555555",
                    dp(6)
                )
            )

            tarjeta.addView(
                texto(
                    "Método: ${pago.metodo_pago ?: "No disponible"}",
                    14f,
                    false,
                    "#555555",
                    dp(4)
                )
            )

            tarjeta.addView(
                texto(
                    "Contrato #${pago.id_contrato} · ${pago.estado ?: "Sin estado"}",
                    13f,
                    true,
                    "#2E7D32"
                ).apply {
                    EvohkaUi.aplicarEstado(this, pago.estado)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            )

            if (!pago.observaciones.isNullOrBlank()) {
                tarjeta.addView(
                    texto(
                        "Observaciones: ${pago.observaciones}",
                        14f,
                        false,
                        "#444444",
                        dp(8)
                    )
                )
            }

            contenedor.addView(tarjeta)
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

    private fun abrirRegistroPago() {
        val id = idContrato ?: return

        val intentRegistro = Intent(
            this,
            RegistrarPagoActivity::class.java
        ).apply {
            putExtra("id_contrato", id)
            putExtra(
                "titulo_propiedad",
                intent.getStringExtra("titulo_propiedad")
                    ?: "Inmueble"
            )
            putExtra(
                "valor_mensual",
                intent.getDoubleExtra(
                    "valor_mensual",
                    0.0
                )
            )
        }

        startActivity(intentRegistro)
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

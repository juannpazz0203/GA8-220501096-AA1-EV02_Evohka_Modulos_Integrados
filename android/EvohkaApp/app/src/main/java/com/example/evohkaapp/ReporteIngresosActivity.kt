package com.example.evohkaapp

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReporteIngresosActivity : Activity() {

    private lateinit var txtTotal: TextView
    private lateinit var txtCantidad: TextView
    private lateinit var txtEstado: TextView
    private lateinit var contenedor: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_ingresos)

        txtTotal = findViewById(R.id.txtTotalIngresos)
        txtCantidad = findViewById(R.id.txtCantidadPagosReporte)
        txtEstado = findViewById(R.id.txtEstadoReporte)
        contenedor = findViewById(R.id.contenedorReporte)

        NavegacionEvohka.configurarPropietario(
            this,
            null
        )

        cargarReporte()
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density)
            .toInt()
    }

    private fun cargarReporte() {
        val preferencias = getSharedPreferences(
            "EvohkaSesion",
            MODE_PRIVATE
        )

        val token = preferencias.getString("token", null)
        val rol = preferencias.getString("rol", null)

        if (token.isNullOrBlank() || rol != "propietario") {
            txtEstado.text = "Sesión de propietario no válida"
            return
        }

        txtEstado.text = "Generando reporte..."

        RetrofitClient.apiService
            .obtenerReporteIngresos(token)
            .enqueue(
                object : Callback<RespuestaReporteIngresos> {
                    override fun onResponse(
                        call: Call<RespuestaReporteIngresos>,
                        response: Response<RespuestaReporteIngresos>
                    ) {
                        val reporte = response.body()?.datos

                        if (response.isSuccessful && reporte != null) {
                            mostrarReporte(reporte)
                        } else {
                            txtEstado.text =
                                "No se pudo generar el reporte"

                            Toast.makeText(
                                this@ReporteIngresosActivity,
                                mensajeError(
                                    response,
                                    "No se pudo generar el reporte"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RespuestaReporteIngresos>,
                        t: Throwable
                    ) {
                        txtEstado.text = "Error de conexión"

                        Toast.makeText(
                            this@ReporteIngresosActivity,
                            "No se pudo conectar con el servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    private fun mostrarReporte(reporte: ReporteIngresos) {
        txtTotal.text =
            "Total: ${FormatoEvohka.moneda(reporte.resumen.total_ingresos)}"

        txtCantidad.text =
            "${reporte.resumen.cantidad_pagos} pago(s) registrado(s)"

        contenedor.removeAllViews()

        if (reporte.detalle.isEmpty()) {
            txtEstado.text =
                "Aún no hay ingresos registrados"
            return
        }

        txtEstado.text =
            "Ingresos calculados con pagos reales registrados"

        contenedor.addView(
            tituloSeccion("Ingresos por propiedad")
        )

        reporte.por_propiedad.forEach { ingreso ->
            val tarjeta = tarjetaBase()

            tarjeta.addView(
                texto(
                    ingreso.titulo_propiedad ?: "Inmueble",
                    16f,
                    true,
                    "#202020"
                )
            )

            tarjeta.addView(
                texto(
                    FormatoEvohka.moneda(
                        ingreso.total_ingresos
                    ),
                    18f,
                    true,
                    "#7B2FF7",
                    dp(6)
                )
            )

            tarjeta.addView(
                texto(
                    "${ingreso.cantidad_pagos} pago(s)",
                    14f,
                    false,
                    "#555555",
                    dp(4)
                )
            )

            contenedor.addView(tarjeta)
        }

        contenedor.addView(
            tituloSeccion("Detalle de pagos")
        )

        reporte.detalle.forEach { detalle ->
            val tarjeta = tarjetaBase()

            tarjeta.addView(
                texto(
                    detalle.titulo_propiedad ?: "Inmueble",
                    15f,
                    true,
                    "#202020"
                )
            )

            tarjeta.addView(
                texto(
                    FormatoEvohka.moneda(detalle.monto),
                    17f,
                    true,
                    "#7B2FF7",
                    dp(5)
                )
            )

            tarjeta.addView(
                texto(
                    "${FormatoEvohka.fecha(detalle.fecha_pago)} · ${detalle.metodo_pago ?: "Sin método"}",
                    13f,
                    false,
                    "#555555",
                    dp(4)
                )
            )

            tarjeta.addView(
                texto(
                    "Contrato #${detalle.id_contrato} · ${detalle.estado ?: "Sin estado"}",
                    13f,
                    false,
                    "#666666",
                    dp(3)
                )
            )

            contenedor.addView(tarjeta)
        }
    }

    private fun tituloSeccion(contenido: String): TextView {
        return texto(
            contenido,
            17f,
            true,
            "#202020",
            dp(14)
        ).apply {
            setPadding(0, dp(14), 0, dp(10))
        }
    }

    private fun tarjetaBase(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundResource(R.drawable.bg_card_evohka)
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
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

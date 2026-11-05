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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FallasActivity : EvohkaActivity() {

    private lateinit var contenedor: LinearLayout
    private lateinit var txtEstado: TextView
    private lateinit var rol: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fallas)

        contenedor = findViewById(R.id.contenedorFallas)
        txtEstado = findViewById(R.id.txtEstadoFallas)
        rol = getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
            .getString("rol", "")
            .orEmpty()

        findViewById<Button>(R.id.btnReportarFalla).apply {
            visibility = if (rol == "arrendatario") View.VISIBLE else View.GONE
            setOnClickListener {
                startActivity(Intent(this@FallasActivity, ReportarFallaActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cargarFallas()
    }

    private fun cargarFallas() {
        val token = getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
            .getString("token", null)
        if (token.isNullOrBlank()) {
            txtEstado.text = "Sesión no válida"
            return
        }

        txtEstado.text = "Consultando fallas..."
        RetrofitClient.apiService.listarFallas(token).enqueue(
            object : Callback<RespuestaFallas> {
                override fun onResponse(
                    call: Call<RespuestaFallas>,
                    response: Response<RespuestaFallas>
                ) {
                    if (response.isSuccessful) {
                        mostrarFallas(response.body()?.datos ?: emptyList())
                    } else {
                        txtEstado.text = "No se pudieron consultar las fallas"
                    }
                }

                override fun onFailure(call: Call<RespuestaFallas>, t: Throwable) {
                    txtEstado.text = "No se pudo conectar con el servidor"
                }
            }
        )
    }

    private fun mostrarFallas(fallas: List<Falla>) {
        contenedor.removeAllViews()
        txtEstado.text = if (fallas.isEmpty()) {
            "No hay fallas registradas"
        } else {
            "${fallas.size} falla(s) encontrada(s)"
        }

        fallas.forEach { falla ->
            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                setBackgroundResource(R.drawable.bg_card_evohka)
            }
            tarjeta.addView(texto(falla.titulo_propiedad ?: "Inmueble", 18f, true))
            tarjeta.addView(texto("${falla.categoria} · ${FormatoEvohka.fecha(falla.fecha_reporte)}", 14f, true))
            tarjeta.addView(texto(falla.descripcion, 14f, false))
            if (rol == "propietario") {
                val nombre = listOfNotNull(
                    falla.nombre_arrendatario,
                    falla.apellido_arrendatario
                ).joinToString(" ").ifBlank { "Arrendatario" }
                tarjeta.addView(texto("Reportado por: $nombre", 13f, false))
            }
            val estado = falla.estado.lowercase()
            tarjeta.addView(
                texto(
                    "Estado: ${estado.replace('_', ' ')}",
                    14f,
                    true
                ).apply {
                    EvohkaUi.aplicarEstado(this, estado)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            )

            if (rol == "propietario" && estado != "resuelta") {
                val siguiente = if (estado == "pendiente") "en_proceso" else "resuelta"
                tarjeta.addView(Button(this).apply {
                    text = if (siguiente == "en_proceso") "Marcar en proceso" else "Marcar resuelta"
                    isAllCaps = false
                    setBackgroundResource(R.drawable.bg_button_evohka)
                    backgroundTintList = null
                    setOnClickListener { actualizarEstado(falla.id_falla, siguiente) }
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
                ).apply { topMargin = dp(10) })
            }

            contenedor.addView(tarjeta, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) })
        }
    }

    private fun actualizarEstado(idFalla: Int, estado: String) {
        val token = getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
            .getString("token", null) ?: return
        RetrofitClient.apiService.actualizarEstadoFalla(
            token,
            idFalla,
            ActualizarEstadoFallaRequest(estado)
        ).enqueue(object : Callback<RespuestaFalla> {
            override fun onResponse(
                call: Call<RespuestaFalla>,
                response: Response<RespuestaFalla>
            ) {
                Toast.makeText(
                    this@FallasActivity,
                    if (response.isSuccessful) "Estado actualizado" else "No se pudo actualizar la falla",
                    Toast.LENGTH_SHORT
                ).show()
                if (response.isSuccessful) cargarFallas()
            }

            override fun onFailure(call: Call<RespuestaFalla>, t: Throwable) {
                Toast.makeText(this@FallasActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun texto(valor: String, tamano: Float, negrita: Boolean) =
        TextView(this).apply {
            text = valor
            textSize = tamano
            setTextColor(Color.parseColor("#303030"))
            setPadding(0, dp(4), 0, dp(4))
            if (negrita) setTypeface(null, Typeface.BOLD)
        }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}

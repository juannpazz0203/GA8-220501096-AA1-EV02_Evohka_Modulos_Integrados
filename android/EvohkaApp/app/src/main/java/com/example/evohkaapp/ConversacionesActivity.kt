package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConversacionesActivity : Activity() {

    private lateinit var txtEstado: TextView
    private lateinit var contenedor: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversaciones)
        txtEstado = findViewById(R.id.txtEstadoConversaciones)
        contenedor = findViewById(R.id.contenedorConversaciones)
    }

    override fun onResume() {
        super.onResume()
        cargarConversaciones()
    }

    private fun cargarConversaciones() {
        val token = getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
            .getString("token", null)
        if (token.isNullOrBlank()) {
            txtEstado.text = "Sesión no válida"
            return
        }
        txtEstado.text = "Consultando conversaciones..."
        RetrofitClient.apiService.listarConversaciones(token).enqueue(
            object : Callback<RespuestaConversaciones> {
                override fun onResponse(
                    call: Call<RespuestaConversaciones>,
                    response: Response<RespuestaConversaciones>
                ) {
                    if (response.isSuccessful) {
                        mostrar(response.body()?.datos ?: emptyList())
                    } else {
                        txtEstado.text = "No se pudieron consultar las conversaciones"
                    }
                }

                override fun onFailure(call: Call<RespuestaConversaciones>, t: Throwable) {
                    txtEstado.text = "No se pudo conectar con el servidor"
                }
            }
        )
    }

    private fun mostrar(conversaciones: List<Conversacion>) {
        contenedor.removeAllViews()
        txtEstado.text = if (conversaciones.isEmpty()) {
            "No hay relaciones contractuales disponibles para conversar"
        } else {
            "${conversaciones.size} conversación(es)"
        }
        conversaciones.forEach { conversacion ->
            val nombre = listOfNotNull(
                conversacion.nombre_otro_usuario,
                conversacion.apellido_otro_usuario
            ).joinToString(" ").ifBlank { "Usuario" }
            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                setBackgroundResource(R.drawable.bg_card_evohka)
                isClickable = true
                isFocusable = true
                setOnClickListener { abrirChat(conversacion, nombre) }
            }
            tarjeta.addView(TextView(this).apply {
                text = conversacion.titulo_propiedad
                textSize = 17f
                setTextColor(Color.parseColor("#202020"))
                setTypeface(null, Typeface.BOLD)
            })
            tarjeta.addView(TextView(this).apply {
                text = nombre
                textSize = 14f
                setTextColor(Color.parseColor("#444444"))
                setPadding(0, dp(5), 0, 0)
            })
            tarjeta.addView(TextView(this).apply {
                text = conversacion.ultimo_mensaje ?: "Toca para iniciar la conversación"
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
                setPadding(0, dp(8), 0, 0)
            })
            if (conversacion.no_leidos > 0) {
                tarjeta.addView(TextView(this).apply {
                    text = "${conversacion.no_leidos} mensaje(s) sin leer"
                    setTextColor(Color.parseColor("#7B2FF7"))
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, dp(6), 0, 0)
                })
            }
            contenedor.addView(tarjeta, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) })
        }
    }

    private fun abrirChat(conversacion: Conversacion, nombre: String) {
        startActivity(Intent(this, ChatActivity::class.java).apply {
            putExtra("id_propiedad", conversacion.id_propiedad)
            putExtra("id_otro_usuario", conversacion.id_otro_usuario)
            putExtra("titulo_propiedad", conversacion.titulo_propiedad)
            putExtra("nombre_otro_usuario", nombre)
        })
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}

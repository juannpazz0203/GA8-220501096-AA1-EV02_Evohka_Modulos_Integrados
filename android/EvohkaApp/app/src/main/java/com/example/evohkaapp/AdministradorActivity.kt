package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdministradorActivity : EvohkaActivity() {

    private lateinit var contenedor: LinearLayout
    private lateinit var txtEstado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_administrador)
        contenedor = findViewById(R.id.contenedorAdmin)
        txtEstado = findViewById(R.id.txtEstadoAdmin)

        findViewById<Button>(R.id.btnFaqAdmin).setOnClickListener {
            startActivity(Intent(this, FaqActivity::class.java))
        }
        findViewById<Button>(R.id.btnCerrarSesionAdmin).setOnClickListener {
            getSharedPreferences("EvohkaSesion", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        cargarResumen()
    }

    private fun cargarResumen() {
        val token = getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
            .getString("token", null)
        if (token.isNullOrBlank()) {
            txtEstado.text = "Sesión no válida"
            return
        }
        RetrofitClient.apiService.obtenerResumenAdministrador(token).enqueue(
            object : Callback<RespuestaAdmin> {
                override fun onResponse(
                    call: Call<RespuestaAdmin>,
                    response: Response<RespuestaAdmin>
                ) {
                    val datos = response.body()?.datos
                    if (response.isSuccessful && datos != null) {
                        mostrar(datos)
                    } else {
                        txtEstado.text = if (response.code() == 403) {
                            "La sesión no tiene permisos de administrador"
                        } else {
                            "No se pudo consultar el panel"
                        }
                    }
                }

                override fun onFailure(call: Call<RespuestaAdmin>, t: Throwable) {
                    txtEstado.text = "No se pudo conectar con el servidor"
                }
            }
        )
    }

    private fun mostrar(datos: AdminResumen) {
        contenedor.removeAllViews()
        txtEstado.text = "Vista de solo lectura · últimos 50 registros por sección"
        val t = datos.totales
        resumen(
            listOf(
                "Usuarios" to t.usuarios,
                "Propiedades" to t.propiedades,
                "Solicitudes" to t.solicitudes,
                "Contratos" to t.contratos,
                "Pagos" to t.pagos,
                "Fallas" to t.fallas,
                "Mensajes" to t.mensajes,
                "Notificaciones" to t.notificaciones
            )
        )
        seccion("Usuarios", datos.usuarios.map {
            "#${it.id_usuario} ${it.nombre} ${it.apellido.orEmpty()} · ${it.rol} · ${it.correo}"
        })
        seccion("Propiedades", datos.propiedades.map {
            "#${it.id_propiedad} ${it.titulo} · ${it.ciudad.orEmpty()} · ${FormatoEvohka.moneda(it.canon)}"
        })
        seccion("Solicitudes", datos.solicitudes.map {
            "#${it.id_solicitud} ${it.titulo_propiedad.orEmpty()} · ${it.estado} · ${FormatoEvohka.fecha(it.fecha_solicitud)}"
        })
        seccion("Contratos", datos.contratos.map {
            "#${it.id_contrato} ${it.titulo_propiedad.orEmpty()} · ${it.estado} · ${FormatoEvohka.fecha(it.fecha_inicio)}"
        })
        seccion("Fallas", datos.fallas.map {
            "#${it.id_falla} ${it.titulo_propiedad.orEmpty()} · ${it.categoria} · ${it.estado}"
        })
    }

    private fun resumen(metricas: List<Pair<String, Int>>) {
        contenedor.addView(TextView(this).apply {
            text = "Resumen general"
            textSize = 19f
            setTextColor(Color.parseColor("#211A24"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        })

        metricas.chunked(2).forEach { pareja ->
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            pareja.forEachIndexed { indice, metrica ->
                fila.addView(
                    TextView(this).apply {
                        text = "${metrica.second}\n${metrica.first}"
                        gravity = Gravity.CENTER
                        textSize = 15f
                        setTextColor(Color.parseColor("#211A24"))
                        setTypeface(null, Typeface.BOLD)
                        setBackgroundResource(R.drawable.bg_nav_item_active)
                        setPadding(dp(10), dp(16), dp(10), dp(16))
                    },
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        if (indice > 0) marginStart = dp(10)
                    }
                )
            }

            contenedor.addView(
                fila,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            )
        }
    }

    private fun seccion(titulo: String, filas: List<String>) {
        val bloque = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundResource(R.drawable.bg_card_evohka)
        }
        bloque.addView(TextView(this).apply {
            text = titulo
            textSize = 18f
            setTextColor(Color.parseColor("#7B2FF7"))
            setTypeface(null, Typeface.BOLD)
        })
        if (filas.isEmpty()) {
            bloque.addView(fila("Sin registros"))
        } else {
            filas.forEach { valor ->
                bloque.addView(
                    fila(valor),
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(8)
                    }
                )
            }
        }
        contenedor.addView(bloque, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(14) })
    }

    private fun fila(valor: String) = TextView(this).apply {
        text = valor
        textSize = 13f
        setTextColor(Color.parseColor("#303030"))
        setBackgroundResource(R.drawable.bg_button_secondary_evohka)
        setPadding(dp(12), dp(10), dp(12), dp(10))
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}

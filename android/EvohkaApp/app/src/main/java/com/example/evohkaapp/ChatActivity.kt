package com.example.evohkaapp

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatActivity : EvohkaActivity() {

    private lateinit var contenedor: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var txtEstado: TextView
    private lateinit var editMensaje: EditText
    private lateinit var btnEnviar: Button
    private var idPropiedad = 0
    private var idOtroUsuario = 0
    private var idUsuario = 0
    private var nombreOtroUsuario = "Usuario"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        aplicarInsetsSistema(incluirTeclado = true)

        idPropiedad = intent.getIntExtra("id_propiedad", 0)
        idOtroUsuario = intent.getIntExtra("id_otro_usuario", 0)
        val preferencias = getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
        idUsuario = preferencias.getInt("id_usuario", 0)
        nombreOtroUsuario = intent.getStringExtra("nombre_otro_usuario")
            ?: "Usuario"

        contenedor = findViewById(R.id.contenedorMensajes)
        scroll = findViewById(R.id.scrollChat)
        txtEstado = findViewById(R.id.txtEstadoChat)
        editMensaje = findViewById(R.id.editMensaje)
        btnEnviar = findViewById(R.id.btnEnviarMensaje)
        findViewById<TextView>(R.id.txtTituloChat).text =
            "$nombreOtroUsuario · " +
                (intent.getStringExtra("titulo_propiedad") ?: "Inmueble")

        btnEnviar.setOnClickListener { enviar() }
        editMensaje.setOnFocusChangeListener { _, tieneFoco ->
            if (tieneFoco) {
                desplazarAlFinal()
            }
        }
        findViewById<LinearLayout>(
            R.id.zonaComposicionChat
        ).addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (editMensaje.hasFocus()) {
                desplazarAlFinal()
            }
        }
        cargarMensajes()
    }

    private fun token() = SesionSegura.obtenerToken()

    private fun cargarMensajes() {
        val sesion = token()
        if (sesion.isNullOrBlank() || idPropiedad <= 0 || idOtroUsuario <= 0) {
            txtEstado.text = "Conversación no válida"
            return
        }
        RetrofitClient.apiService.listarMensajes(
            sesion,
            idPropiedad,
            idOtroUsuario
        ).enqueue(object : Callback<RespuestaMensajes> {
            override fun onResponse(
                call: Call<RespuestaMensajes>,
                response: Response<RespuestaMensajes>
            ) {
                if (response.isSuccessful) {
                    mostrar(response.body()?.datos ?: emptyList())
                    marcarLeidos(sesion)
                } else {
                    txtEstado.text = "No se pudo abrir la conversación"
                }
            }

            override fun onFailure(call: Call<RespuestaMensajes>, t: Throwable) {
                txtEstado.text = "No se pudo conectar con el servidor"
            }
        })
    }

    private fun mostrar(mensajes: List<Mensaje>) {
        contenedor.removeAllViews()
        txtEstado.text = if (mensajes.isEmpty()) "Aún no hay mensajes" else ""
        mensajes.forEach { mensaje ->
            val propio = mensaje.id_remitente == idUsuario
            val anchoMaximo = minOf(
                (resources.displayMetrics.widthPixels * 0.78f).toInt(),
                dp(520)
            )
            val burbuja = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(10))
                setBackgroundResource(
                    if (propio) {
                        R.drawable.bg_bubble_own_evohka
                    } else {
                        R.drawable.bg_bubble_received_evohka
                    }
                )
            }

            burbuja.addView(TextView(this).apply {
                text = if (propio) "Tú" else nombreOtroUsuario
                textSize = 12f
                setTextColor(Color.parseColor("#7B2FF7"))
                setTypeface(null, Typeface.BOLD)
                maxWidth = anchoMaximo
            })
            burbuja.addView(TextView(this).apply {
                text = mensaje.contenido
                textSize = 15f
                setTextColor(Color.parseColor("#211A24"))
                setPadding(0, dp(4), 0, dp(4))
                maxWidth = anchoMaximo
            })
            burbuja.addView(TextView(this).apply {
                text = FormatoEvohka.fechaHora(mensaje.fecha_envio)
                textSize = 11f
                gravity = Gravity.END
                setTextColor(Color.parseColor("#6E6672"))
                maxWidth = anchoMaximo
            })

            contenedor.addView(burbuja, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (propio) Gravity.END else Gravity.START
                bottomMargin = dp(8)
                marginStart = if (propio) dp(45) else 0
                marginEnd = if (propio) 0 else dp(45)
            })
        }
        desplazarAlFinal()
    }

    private fun enviar() {
        val contenido = editMensaje.text.toString().trim()
        val sesion = token()
        if (contenido.isBlank() || sesion.isNullOrBlank()) return
        btnEnviar.isEnabled = false
        RetrofitClient.apiService.enviarMensaje(
            sesion,
            EnviarMensajeRequest(idOtroUsuario, idPropiedad, contenido)
        ).enqueue(object : Callback<RespuestaMensaje> {
            override fun onResponse(
                call: Call<RespuestaMensaje>,
                response: Response<RespuestaMensaje>
            ) {
                btnEnviar.isEnabled = true
                if (response.isSuccessful) {
                    editMensaje.text.clear()
                    cargarMensajes()
                } else {
                    txtEstado.text = "No fue posible enviar el mensaje"
                }
            }

            override fun onFailure(call: Call<RespuestaMensaje>, t: Throwable) {
                btnEnviar.isEnabled = true
                txtEstado.text = "No se pudo conectar con el servidor"
            }
        })
    }

    private fun marcarLeidos(sesion: String) {
        RetrofitClient.apiService.marcarMensajesLeidos(
            sesion,
            MarcarMensajesLeidosRequest(idPropiedad, idOtroUsuario)
        ).enqueue(object : Callback<RespuestaOperacionMensajes> {
            override fun onResponse(
                call: Call<RespuestaOperacionMensajes>,
                response: Response<RespuestaOperacionMensajes>
            ) = Unit

            override fun onFailure(call: Call<RespuestaOperacionMensajes>, t: Throwable) = Unit
        })
    }

    private fun desplazarAlFinal() {
        scroll.post {
            scroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}

package com.example.evohkaapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
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

class ContratosActivity : EvohkaActivity() {

    private lateinit var txtEstado: TextView
    private lateinit var contenedor: LinearLayout
    private var actualizando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contratos)

        txtEstado = findViewById(R.id.txtEstadoContratos)
        contenedor = findViewById(R.id.contenedorContratos)

        NavegacionEvohka.configurarSegunRol(
            this,
            R.id.contenedorNavegacionContratos,
            NavegacionEvohka.DestinoPropietario.CONTRATOS,
            NavegacionEvohka.DestinoArrendatario.CONTRATOS
        )
    }

    override fun onResume() {
        super.onResume()
        cargarContratos()
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density)
            .toInt()
    }

    private fun cargarContratos() {
        val preferencias = getSharedPreferences(
            "EvohkaSesion",
            MODE_PRIVATE
        )

        val token = preferencias.getString(
            "token",
            null
        )

        val rol = preferencias.getString(
            "rol",
            null
        )

        if (
            token.isNullOrBlank() ||
            (rol != "propietario" && rol != "arrendatario")
        ) {
            txtEstado.text = "Sesión no válida"
            return
        }

        txtEstado.text = "Consultando contratos..."

        RetrofitClient.apiService.listarContratos(token)
            .enqueue(
                object : Callback<RespuestaContratos> {
                    override fun onResponse(
                        call: Call<RespuestaContratos>,
                        response: Response<RespuestaContratos>
                    ) {
                        if (response.isSuccessful) {
                            mostrarContratos(
                                response.body()?.datos
                                    ?: emptyList(),
                                rol
                            )
                        } else {
                            txtEstado.text =
                                "No se pudieron consultar los contratos"

                            Toast.makeText(
                                this@ContratosActivity,
                                mensajeError(
                                    response,
                                    "No se pudieron consultar los contratos"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RespuestaContratos>,
                        t: Throwable
                    ) {
                        txtEstado.text = "Error de conexión"

                        Toast.makeText(
                            this@ContratosActivity,
                            "No se pudo conectar con el servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    private fun mostrarContratos(
        contratos: List<Contrato>,
        rol: String
    ) {
        contenedor.removeAllViews()

        if (contratos.isEmpty()) {
            txtEstado.text =
                if (rol == "propietario") {
                    "Aún no has creado contratos"
                } else {
                    "Aún no tienes contratos"
                }
            return
        }

        txtEstado.text =
            "${contratos.size} contrato(s) encontrado(s)"

        contratos.forEach { contrato ->
            contenedor.addView(
                crearTarjeta(contrato, rol)
            )
        }
    }

    private fun crearTarjeta(
        contrato: Contrato,
        rol: String
    ): LinearLayout {
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
            bottomMargin = dp(16)
        }

        tarjeta.addView(
            texto(
                contrato.titulo_propiedad ?: "Inmueble",
                18f,
                true,
                "#202020"
            )
        )

        val nombreArrendatario = listOfNotNull(
            contrato.nombre_arrendatario
                ?.takeIf { it.isNotBlank() },
            contrato.apellido_arrendatario
                ?.takeIf { it.isNotBlank() }
        ).joinToString(" ").ifBlank { "Arrendatario" }

        tarjeta.addView(
            texto(
                "Arrendatario: $nombreArrendatario",
                14f,
                false,
                "#444444",
                dp(8)
            )
        )

        tarjeta.addView(
            texto(
                "Vigencia: ${FormatoEvohka.fecha(contrato.fecha_inicio)} - ${FormatoEvohka.fecha(contrato.fecha_fin)}",
                14f,
                false,
                "#666666",
                dp(4)
            )
        )

        tarjeta.addView(
            texto(
                "Valor mensual: ${FormatoEvohka.moneda(contrato.valor_mensual)}",
                16f,
                true,
                "#7B2FF7",
                dp(8)
            )
        )

        if (contrato.deposito != null) {
            tarjeta.addView(
                texto(
                    "Depósito: ${FormatoEvohka.moneda(contrato.deposito)}",
                    14f,
                    false,
                    "#444444",
                    dp(4)
                )
            )
        }

        if (!contrato.descripcion.isNullOrBlank()) {
            tarjeta.addView(
                texto(
                    contrato.descripcion,
                    14f,
                    false,
                    "#444444",
                    dp(6)
                )
            )
        }

        val estado = contrato.estado
            ?.trim()
            ?.lowercase()
            ?: "sin estado"

        val txtEstadoContrato = texto(
            "Estado: ${estado.replaceFirstChar { it.uppercase() }}",
            14f,
            true,
            when (estado) {
                "activo" -> "#2E7D32"
                "cancelado" -> "#C62828"
                else -> "#7B2FF7"
            },
            dp(10)
        ).apply {
            EvohkaUi.aplicarEstado(this, estado)
        }

        tarjeta.addView(
            txtEstadoContrato,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val btnPagos = botonPrincipal("Ver historial de pagos")
        btnPagos.setOnClickListener {
            abrirPagos(contrato, false)
        }
        agregarBoton(tarjeta, btnPagos)

        val btnChat = botonSecundario("Abrir conversación")
        btnChat.setOnClickListener {
            abrirChat(contrato, rol)
        }
        agregarBoton(tarjeta, btnChat)

        if (estado == "activo") {
            val btnRegistrar = botonSecundario("Registrar pago")
            btnRegistrar.setOnClickListener {
                abrirPagos(contrato, true)
            }
            agregarBoton(tarjeta, btnRegistrar)
        }

        if (rol == "propietario" && estado == "activo") {
            val acciones = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val btnFinalizar = botonSecundario("Finalizar")
            btnFinalizar.setOnClickListener {
                confirmarCambio(contrato, "finalizado")
            }

            val btnCancelar = botonSecundario("Cancelar")
            btnCancelar.setTextColor(Color.parseColor("#C62828"))
            btnCancelar.setOnClickListener {
                confirmarCambio(contrato, "cancelado")
            }

            acciones.addView(
                btnFinalizar,
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f
                ).apply {
                    marginEnd = dp(5)
                }
            )

            acciones.addView(
                btnCancelar,
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f
                ).apply {
                    marginStart = dp(5)
                }
            )

            tarjeta.addView(
                acciones,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(10)
                }
            )
        }

        return tarjeta
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

    private fun botonPrincipal(texto: String): Button {
        return Button(this).apply {
            text = texto
            isAllCaps = false
            setTextColor(Color.parseColor("#170E0E"))
            setTypeface(null, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_button_evohka)
            backgroundTintList = null
        }
    }

    private fun botonSecundario(texto: String): Button {
        return Button(this).apply {
            text = texto
            isAllCaps = false
            setTextColor(Color.parseColor("#7B2FF7"))
            setBackgroundResource(R.drawable.bg_button_secondary_evohka)
            backgroundTintList = null
        }
    }

    private fun agregarBoton(
        tarjeta: LinearLayout,
        boton: Button
    ) {
        tarjeta.addView(
            boton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(10)
            }
        )
    }

    private fun abrirPagos(
        contrato: Contrato,
        abrirRegistro: Boolean
    ) {
        val destino =
            if (abrirRegistro) {
                RegistrarPagoActivity::class.java
            } else {
                PagosActivity::class.java
            }

        val intent = Intent(this, destino).apply {
            putExtra("id_contrato", contrato.id_contrato)
            putExtra(
                "titulo_propiedad",
                contrato.titulo_propiedad ?: "Inmueble"
            )
            putExtra(
                "valor_mensual",
                contrato.valor_mensual ?: 0.0
            )
            putExtra(
                "estado_contrato",
                contrato.estado ?: ""
            )
        }

        startActivity(intent)
    }

    private fun abrirChat(
        contrato: Contrato,
        rol: String
    ) {
        val idOtroUsuario = if (rol == "propietario") {
            contrato.id_arrendatario
        } else {
            contrato.id_propietario
        }
        val nombre = if (rol == "propietario") {
            listOfNotNull(
                contrato.nombre_arrendatario,
                contrato.apellido_arrendatario
            ).joinToString(" ").ifBlank { "Arrendatario" }
        } else {
            "Propietario"
        }

        startActivity(Intent(this, ChatActivity::class.java).apply {
            putExtra("id_propiedad", contrato.id_propiedad)
            putExtra("id_otro_usuario", idOtroUsuario)
            putExtra("titulo_propiedad", contrato.titulo_propiedad ?: "Inmueble")
            putExtra("nombre_otro_usuario", nombre)
        })
    }

    private fun confirmarCambio(
        contrato: Contrato,
        estado: String
    ) {
        if (actualizando) {
            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                if (estado == "finalizado") {
                    "Finalizar contrato"
                } else {
                    "Cancelar contrato"
                }
            )
            .setMessage(
                "¿Confirmas este cambio para ${contrato.titulo_propiedad ?: "el inmueble"}?"
            )
            .setNegativeButton("Volver", null)
            .setPositiveButton("Confirmar") { _, _ ->
                actualizarEstado(
                    contrato.id_contrato,
                    estado
                )
            }
            .show()
    }

    private fun actualizarEstado(
        idContrato: Int,
        estado: String
    ) {
        val token = getSharedPreferences(
            "EvohkaSesion",
            MODE_PRIVATE
        ).getString("token", null)

        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "La sesión no es válida",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        actualizando = true
        txtEstado.text = "Actualizando contrato..."

        RetrofitClient.apiService.actualizarEstadoContrato(
            token,
            idContrato,
            ActualizarEstadoContratoRequest(estado)
        ).enqueue(
            object : Callback<RespuestaContrato> {
                override fun onResponse(
                    call: Call<RespuestaContrato>,
                    response: Response<RespuestaContrato>
                ) {
                    actualizando = false

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ContratosActivity,
                            response.body()?.mensaje
                                ?: "Contrato actualizado",
                            Toast.LENGTH_LONG
                        ).show()
                        cargarContratos()
                    } else {
                        txtEstado.text =
                            "No se pudo actualizar el contrato"

                        Toast.makeText(
                            this@ContratosActivity,
                            mensajeError(
                                response,
                                "No se pudo actualizar el contrato"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaContrato>,
                    t: Throwable
                ) {
                    actualizando = false
                    txtEstado.text = "Error de conexión"

                    Toast.makeText(
                        this@ContratosActivity,
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
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

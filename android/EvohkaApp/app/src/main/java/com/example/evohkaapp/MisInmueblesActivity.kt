package com.example.evohkaapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class MisInmueblesActivity : Activity() {

    private lateinit var txtEstadoCarga: TextView
    private lateinit var contenedorInmuebles: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_mis_inmuebles)

        txtEstadoCarga =
            findViewById(R.id.txtEstadoCarga)

        contenedorInmuebles =
            findViewById(R.id.contenedorInmuebles)

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
        cargarInmuebles()
    }

    private fun dp(valor: Int): Int {
        return (
            valor * resources.displayMetrics.density
        ).toInt()
    }

    private fun cargarInmuebles() {

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

        contenedorInmuebles.removeAllViews()

        if (token.isNullOrBlank() || idUsuario <= 0) {

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
                                misPropiedades
                            )

                        } else {

                            txtEstadoCarga.text =
                                "No se pudieron consultar los inmuebles"

                            Toast.makeText(
                                this@MisInmueblesActivity,
                                obtenerMensajeError(
                                    response,
                                    "No se pudieron consultar los inmuebles"
                                ),
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
        propiedades: List<Propiedad>
    ) {

        contenedorInmuebles.removeAllViews()

        if (propiedades.isEmpty()) {

            txtEstadoCarga.text =
                "No tienes inmuebles registrados"

            return
        }

        txtEstadoCarga.text =
            "${propiedades.size} inmueble(s) registrado(s)"

        val formatoMoneda =
            NumberFormat.getCurrencyInstance(
                Locale.forLanguageTag("es-CO")
            )

        propiedades.forEach { propiedad ->

            val tarjeta =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.VERTICAL

                    setPadding(
                        dp(18),
                        dp(16),
                        dp(18),
                        dp(16)
                    )

                    setBackgroundResource(
                        R.drawable.bg_card_evohka
                    )

                    elevation =
                        dp(2).toFloat()
                }

            val parametrosTarjeta =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        0,
                        0,
                        0,
                        dp(16)
                    )
                }

            tarjeta.layoutParams =
                parametrosTarjeta

            val titulo =
                TextView(this).apply {
                    text =
                        propiedad.titulo
                            ?: "Inmueble"

                    textSize = 18f

                    setTextColor(
                        Color.parseColor("#202020")
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }

            tarjeta.addView(titulo)

            val ubicacion =
                TextView(this).apply {
                    text =
                        "📍 ${propiedad.ciudad ?: "Sin ciudad"} · ${propiedad.barrio ?: "Sin barrio"}"

                    textSize = 14f

                    setTextColor(
                        Color.parseColor("#666666")
                    )

                    setPadding(
                        0,
                        dp(4),
                        0,
                        dp(8)
                    )
                }

            tarjeta.addView(ubicacion)

            val tipo =
                TextView(this).apply {
                    text =
                        propiedad.tipo_inmueble
                            ?: "Tipo no definido"

                    textSize = 13f

                    setTextColor(
                        Color.parseColor("#7B2FF7")
                    )

                    setBackgroundResource(
                        R.drawable.bg_input_evohka
                    )

                    setPadding(
                        dp(10),
                        dp(5),
                        dp(10),
                        dp(5)
                    )
                }

            tarjeta.addView(
                tipo,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            val canon =
                formatoMoneda.format(
                    propiedad.canon ?: 0.0
                )

            val precio =
                TextView(this).apply {
                    text =
                        "$canon / mes"

                    textSize = 18f

                    setTextColor(
                        Color.parseColor("#A63EE8")
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    setPadding(
                        0,
                        dp(12),
                        0,
                        dp(4)
                    )
                }

            tarjeta.addView(precio)

            val detalles =
                TextView(this).apply {
                    text =
                        "${propiedad.habitaciones ?: 0} habitaciones · ${propiedad.banos ?: 0} baños\n${propiedad.direccion ?: "Dirección no registrada"}"

                    textSize = 14f

                    setTextColor(
                        Color.parseColor("#49454F")
                    )
                }

            tarjeta.addView(detalles)

            val acciones =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER
                }

            val parametrosAcciones =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        0,
                        dp(14),
                        0,
                        0
                    )
                }

            acciones.layoutParams =
                parametrosAcciones

            val btnEditar =
                Button(this).apply {
                    text = "Editar"
                    isAllCaps = false

                    setTextColor(
                        Color.parseColor("#170E0E")
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    setBackgroundResource(
                        R.drawable.bg_button_evohka
                    )

                    backgroundTintList = null

                    setOnClickListener {
                        abrirEdicion(propiedad)
                    }
                }

            acciones.addView(
                btnEditar,
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f
                )
            )

            val btnEliminar =
                Button(this).apply {
                    text = "Eliminar"
                    isAllCaps = false

                    setTextColor(
                        Color.parseColor("#C62828")
                    )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )

                    setBackgroundResource(
                        R.drawable.bg_button_outline_danger_evohka
                    )

                    backgroundTintList = null

                    setOnClickListener {
                        confirmarEliminacion(propiedad)
                    }
                }

            acciones.addView(
                btnEliminar,
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f
                ).apply {
                    marginStart = dp(10)
                }
            )

            tarjeta.addView(acciones)

            contenedorInmuebles.addView(tarjeta)
        }
    }

    private fun abrirEdicion(
        propiedad: Propiedad
    ) {
        val idPropiedad =
            propiedad.id_propiedad

        if (idPropiedad == null || idPropiedad <= 0) {
            Toast.makeText(
                this,
                "No se pudo identificar la propiedad",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val intent =
            Intent(
                this,
                EditarInmuebleActivity::class.java
            ).apply {
                putExtra(
                    "id_propiedad",
                    idPropiedad
                )

                putExtra(
                    "titulo",
                    propiedad.titulo.orEmpty()
                )

                putExtra(
                    "tipo_inmueble",
                    propiedad.tipo_inmueble.orEmpty()
                )

                putExtra(
                    "direccion",
                    propiedad.direccion.orEmpty()
                )

                putExtra(
                    "barrio",
                    propiedad.barrio.orEmpty()
                )

                putExtra(
                    "ciudad",
                    propiedad.ciudad.orEmpty()
                )

                putExtra(
                    "canon",
                    propiedad.canon ?: 0.0
                )

                putExtra(
                    "habitaciones",
                    propiedad.habitaciones ?: 0
                )

                putExtra(
                    "banos",
                    propiedad.banos ?: 0
                )
            }

        startActivity(intent)
    }

    private fun confirmarEliminacion(
        propiedad: Propiedad
    ) {
        val idPropiedad =
            propiedad.id_propiedad

        if (idPropiedad == null || idPropiedad <= 0) {
            Toast.makeText(
                this,
                "No se pudo identificar la propiedad",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar propiedad")
            .setMessage(
                "¿Deseas eliminar ${propiedad.titulo ?: "esta propiedad"}? Esta acción no se puede deshacer."
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarPropiedad(idPropiedad)
            }
            .show()
    }

    private fun eliminarPropiedad(
        idPropiedad: Int
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

        RetrofitClient.apiService
            .eliminarPropiedad(
                token,
                idPropiedad
            )
            .enqueue(
                object : Callback<RespuestaPropiedad> {

                    override fun onResponse(
                        call: Call<RespuestaPropiedad>,
                        response: Response<RespuestaPropiedad>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                this@MisInmueblesActivity,
                                response.body()?.mensaje
                                    ?: "Propiedad eliminada correctamente",
                                Toast.LENGTH_LONG
                            ).show()

                            cargarInmuebles()
                        } else {
                            Toast.makeText(
                                this@MisInmueblesActivity,
                                obtenerMensajeError(
                                    response,
                                    "No se pudo eliminar la propiedad"
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<RespuestaPropiedad>,
                        t: Throwable
                    ) {
                        Toast.makeText(
                            this@MisInmueblesActivity,
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

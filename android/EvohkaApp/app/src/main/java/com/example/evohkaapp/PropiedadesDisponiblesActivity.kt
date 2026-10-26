package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class PropiedadesDisponiblesActivity : Activity() {

    private lateinit var txtEstadoPropiedades: TextView
    private lateinit var contenedorPropiedades: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        crearInterfaz()
        cargarPropiedades()
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    private fun fondoTarjeta(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.WHITE)
            cornerRadius = dp(16).toFloat()
            setStroke(
                dp(1),
                Color.parseColor("#E8D9F0")
            )
        }
    }

    private fun fondoEtiqueta(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#F3E4FC"))
            cornerRadius = dp(20).toFloat()
        }
    }

    private fun fondoBotonCerrar(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.WHITE)
            cornerRadius = dp(10).toFloat()
            setStroke(
                dp(1),
                Color.parseColor("#CE74FF")
            )
        }
    }

    private fun crearInterfaz() {

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(20),
                dp(22),
                dp(20),
                dp(16)
            )

            setBackgroundColor(Color.WHITE)
        }

        val titulo = TextView(this).apply {
            text = "Propiedades disponibles"
            textSize = 20f
            gravity = Gravity.CENTER

            setTextColor(Color.BLACK)

            setTypeface(
                null,
                Typeface.BOLD
            )

            setBackgroundResource(
                R.drawable.bg_title_evohka
            )

            setPadding(
                dp(14),
                dp(7),
                dp(14),
                dp(7)
            )
        }

        val parametrosTitulo =
            LinearLayout.LayoutParams(
                dp(297),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }

        raiz.addView(
            titulo,
            parametrosTitulo
        )

        txtEstadoPropiedades =
            TextView(this).apply {

                text =
                    "Cargando propiedades..."

                textSize = 13f

                gravity = Gravity.CENTER

                setTextColor(
                    Color.parseColor("#828282")
                )

                setPadding(
                    0,
                    dp(14),
                    0,
                    dp(14)
                )
            }

        raiz.addView(
            txtEstadoPropiedades
        )

        contenedorPropiedades =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    0,
                    0,
                    0,
                    dp(10)
                )
            }

        val scrollView =
            ScrollView(this).apply {

                isFillViewport = true
                clipToPadding = false

                addView(
                    contenedorPropiedades
                )
            }

        val parametrosScroll =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                weight = 1f
            }

        raiz.addView(
            scrollView,
            parametrosScroll
        )

        val btnCerrarSesion =
            Button(this).apply {

                text = "Cerrar sesión"
                isAllCaps = false
                textSize = 15f

                setTextColor(
                    Color.parseColor("#7B2FF7")
                )

                background =
                    fondoBotonCerrar()

                setOnClickListener {
                    cerrarSesion()
                }
            }

        val parametrosCerrar =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply {
                setMargins(
                    0,
                    dp(6),
                    0,
                    0
                )
            }

        raiz.addView(
            btnCerrarSesion,
            parametrosCerrar
        )

        setContentView(raiz)
    }

    private fun cargarPropiedades() {

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

        if (token.isNullOrBlank()) {

            txtEstadoPropiedades.text =
                "Sesión no válida"

            Toast.makeText(
                this,
                "Debe iniciar sesión nuevamente",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        txtEstadoPropiedades.text =
            "Consultando propiedades..."

        RetrofitClient.apiService
            .listarPropiedades(token)
            .enqueue(
                object :
                    Callback<RespuestaPropiedades> {

                    override fun onResponse(
                        call:
                            Call<RespuestaPropiedades>,
                        response:
                            Response<RespuestaPropiedades>
                    ) {

                        if (
                            response.isSuccessful
                        ) {

                            val propiedades =
                                response
                                    .body()
                                    ?.datos
                                    ?: emptyList()

                            mostrarPropiedades(
                                propiedades
                            )

                        } else {

                            txtEstadoPropiedades.text =
                                "No se pudieron consultar las propiedades"

                            Toast.makeText(
                                this@PropiedadesDisponiblesActivity,
                                "Error del servidor: ${response.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call:
                            Call<RespuestaPropiedades>,
                        t:
                            Throwable
                    ) {

                        txtEstadoPropiedades.text =
                            "Error de conexión"

                        Toast.makeText(
                            this@PropiedadesDisponiblesActivity,
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

        contenedorPropiedades
            .removeAllViews()

        if (propiedades.isEmpty()) {

            txtEstadoPropiedades.text =
                "No hay propiedades disponibles"

            return
        }

        txtEstadoPropiedades.text =
            "${propiedades.size} propiedades disponibles"

        val formatoMoneda =
            NumberFormat.getCurrencyInstance(
                Locale("es", "CO")
            )

        propiedades.forEach {
                propiedad ->

            val tarjeta =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    background =
                        fondoTarjeta()

                    elevation =
                        dp(3).toFloat()

                    setPadding(
                        dp(18),
                        dp(16),
                        dp(18),
                        dp(16)
                    )
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
                        dp(14)
                    )
                }

            tarjeta.layoutParams =
                parametrosTarjeta

            val tituloPropiedad =
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

            tarjeta.addView(
                tituloPropiedad
            )

            val ubicacion =
                TextView(this).apply {

                    val ciudad =
                        propiedad.ciudad
                            ?: "Sin ciudad"

                    val barrio =
                        propiedad.barrio
                            ?: "Sin barrio"

                    text =
                        "$ciudad · $barrio"

                    textSize = 14f

                    setTextColor(
                        Color.parseColor("#666666")
                    )

                    setPadding(
                        0,
                        dp(3),
                        0,
                        dp(8)
                    )
                }

            tarjeta.addView(
                ubicacion
            )

            val tipo =
                TextView(this).apply {

                    text =
                        propiedad.tipo_inmueble
                            ?: "Inmueble"

                    textSize = 13f

                    setTextColor(
                        Color.parseColor("#7B2FF7")
                    )

                    background =
                        fondoEtiqueta()

                    setPadding(
                        dp(10),
                        dp(5),
                        dp(10),
                        dp(5)
                    )
                }

            val parametrosTipo =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            tarjeta.addView(
                tipo,
                parametrosTipo
            )

            val caracteristicas =
                TextView(this).apply {

                    text =
                        "🛏 ${propiedad.habitaciones ?: 0} Hab   ·   🚿 ${propiedad.banos ?: 0} Baños"

                    textSize = 14f

                    setTextColor(
                        Color.parseColor("#444444")
                    )

                    setPadding(
                        0,
                        dp(12),
                        0,
                        dp(4)
                    )
                }

            tarjeta.addView(
                caracteristicas
            )

            val direccion =
                TextView(this).apply {

                    text =
                        propiedad.direccion
                            ?: "Dirección no registrada"

                    textSize = 13f

                    setTextColor(
                        Color.parseColor("#828282")
                    )

                    setPadding(
                        0,
                        0,
                        0,
                        dp(8)
                    )
                }

            tarjeta.addView(
                direccion
            )

            val canon =
                formatoMoneda.format(
                    propiedad.canon
                        ?: 0.0
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
                }

            tarjeta.addView(
                precio
            )

            val btnSeleccionar =
                Button(this).apply {

                    text =
                        "Ver detalle"

                    isAllCaps =
                        false

                    textSize =
                        15f

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

                    backgroundTintList =
                        null

                    setOnClickListener {

                        abrirDetalle(
                            propiedad
                        )
                    }
                }

            val parametrosBoton =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(44)
                ).apply {

                    setMargins(
                        0,
                        dp(14),
                        0,
                        0
                    )
                }

            tarjeta.addView(
                btnSeleccionar,
                parametrosBoton
            )

            contenedorPropiedades
                .addView(
                    tarjeta
                )
        }
    }

    private fun abrirDetalle(
        propiedad: Propiedad
    ) {

        val intent =
            Intent(
                this,
                DetallePropiedadActivity::class.java
            )

        intent.putExtra(
            "id_propiedad",
            propiedad.id_propiedad
                ?: 0
        )

        intent.putExtra(
            "titulo",
            propiedad.titulo
                ?: ""
        )

        intent.putExtra(
            "tipo_inmueble",
            propiedad.tipo_inmueble
                ?: ""
        )

        intent.putExtra(
            "direccion",
            propiedad.direccion
                ?: ""
        )

        intent.putExtra(
            "barrio",
            propiedad.barrio
                ?: ""
        )

        intent.putExtra(
            "ciudad",
            propiedad.ciudad
                ?: ""
        )

        intent.putExtra(
            "canon",
            propiedad.canon
                ?: 0.0
        )

        intent.putExtra(
            "habitaciones",
            propiedad.habitaciones
                ?: 0
        )

        intent.putExtra(
            "banos",
            propiedad.banos
                ?: 0
        )

        intent.putExtra(
            "id_propietario",
            propiedad.id_propietario
                ?: 0
        )

        startActivity(intent)
    }

    private fun cerrarSesion() {

        val preferencias =
            getSharedPreferences(
                "EvohkaSesion",
                MODE_PRIVATE
            )

        preferencias.edit()
            .clear()
            .apply()

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }
}
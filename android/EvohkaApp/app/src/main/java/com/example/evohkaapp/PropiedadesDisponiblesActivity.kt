package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
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

    private fun crearInterfaz() {

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 30)
            setBackgroundColor(
                android.graphics.Color.WHITE
            )
        }

        val titulo = TextView(this).apply {
            text = "Propiedades disponibles"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(
                android.graphics.Color.parseColor("#202020")
            )
            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )
        }

        txtEstadoPropiedades = TextView(this).apply {
            text = "Cargando propiedades..."
            textSize = 14f
            setTextColor(
                android.graphics.Color.parseColor("#666666")
            )
            setPadding(0, 20, 0, 20)
        }

        contenedorPropiedades = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            addView(contenedorPropiedades)
        }

        val parametrosScroll =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                weight = 1f
            }

        val btnCerrarSesion = Button(this).apply {

            text = "Cerrar sesión"
            isAllCaps = false

            setOnClickListener {
                cerrarSesion()
            }
        }

        raiz.addView(titulo)
        raiz.addView(txtEstadoPropiedades)
        raiz.addView(
            scrollView,
            parametrosScroll
        )
        raiz.addView(btnCerrarSesion)

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

        if (token.isNullOrEmpty()) {

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
                object : Callback<RespuestaPropiedades> {

                    override fun onResponse(
                        call: Call<RespuestaPropiedades>,
                        response: Response<RespuestaPropiedades>
                    ) {

                        if (response.isSuccessful) {

                            val propiedades =
                                response.body()?.datos
                                    ?: emptyList()

                            mostrarPropiedades(propiedades)

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
                        call: Call<RespuestaPropiedades>,
                        t: Throwable
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

        contenedorPropiedades.removeAllViews()

        val disponibles =
            propiedades.filter {
                it.estado.equals(
                    "disponible",
                    ignoreCase = true
                )
            }

        if (disponibles.isEmpty()) {

            txtEstadoPropiedades.text =
                "No hay propiedades disponibles"

            return
        }

        txtEstadoPropiedades.text =
            "Propiedades encontradas: ${disponibles.size}"

        val formatoMoneda =
            NumberFormat.getCurrencyInstance(
                Locale("es", "CO")
            )

        disponibles.forEach { propiedad ->

            val tarjeta =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.VERTICAL

                    setPadding(
                        25,
                        25,
                        25,
                        25
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
                        30
                    )
                }

            tarjeta.layoutParams =
                parametrosTarjeta

            val precio =
                formatoMoneda.format(
                    propiedad.precio_mensual ?: 0
                )

            val informacion =
                TextView(this).apply {

                    text = """
                        ${propiedad.titulo ?: "Inmueble"}
                        
                        Ciudad: ${propiedad.ciudad ?: "Sin ciudad"}
                        Dirección: ${propiedad.direccion ?: "Sin dirección"}
                        Canon mensual: $precio
                        Estado: ${propiedad.estado ?: "Disponible"}
                    """.trimIndent()

                    textSize = 16f

                    setTextColor(
                        android.graphics.Color.parseColor(
                            "#202020"
                        )
                    )
                }

            val btnSeleccionar =
                Button(this).apply {

                    text = "Seleccionar"
                    isAllCaps = false

                    setOnClickListener {

                        val intent = Intent(
                            this@PropiedadesDisponiblesActivity,
                            DetallePropiedadActivity::class.java
                        )

                        intent.putExtra(
                            "titulo",
                            propiedad.titulo
                        )

                        intent.putExtra(
                            "ciudad",
                            propiedad.ciudad
                        )

                        intent.putExtra(
                            "direccion",
                            propiedad.direccion
                        )

                        intent.putExtra(
                            "precio_mensual",
                            propiedad.precio_mensual
                        )

                        intent.putExtra(
                            "estado",
                            propiedad.estado
                        )

                        startActivity(intent)
                    }
                }

            tarjeta.addView(informacion)
            tarjeta.addView(btnSeleccionar)

            contenedorPropiedades.addView(tarjeta)
        }
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

        val intent = Intent(
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
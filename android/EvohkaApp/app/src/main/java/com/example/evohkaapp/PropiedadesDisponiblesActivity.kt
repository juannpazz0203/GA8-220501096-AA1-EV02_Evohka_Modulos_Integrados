package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class PropiedadesDisponiblesActivity : EvohkaActivity() {

    private lateinit var txtEstadoPropiedades: TextView
    private lateinit var contenedorPropiedades: LinearLayout
    private lateinit var etBuscarPropiedad: EditText
    private lateinit var spinnerCiudad: Spinner
    private lateinit var spinnerTipoInmueble: Spinner
    private lateinit var etPrecioMinimo: EditText
    private lateinit var etPrecioMaximo: EditText
    private var propiedadesCargadas: List<Propiedad> = emptyList()
    private var tokenCatalogo: String? = null
    private var cambioProgramaticoFiltros = false

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
            setColor(Color.parseColor("#FCFAFD"))
            cornerRadius = dp(18).toFloat()
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
            setColor(Color.parseColor("#FFF5F5"))
            cornerRadius = dp(16).toFloat()
            setStroke(
                dp(1),
                Color.parseColor("#C62828")
            )
        }
    }

    private fun crearCampoFiltro(
        sugerencia: String,
        tipoEntrada: Int
    ): EditText = EditText(this).apply {
        hint = sugerencia
        textSize = 14f
        setTextColor(Color.parseColor("#211A24"))
        setHintTextColor(Color.parseColor("#777077"))
        setBackgroundResource(R.drawable.bg_input_evohka)
        setPadding(dp(14), 0, dp(14), 0)
        inputType = tipoEntrada
        isSingleLine = true
    }

    private fun crearSelectorFiltro(): Spinner = Spinner(this).apply {
        setBackgroundResource(R.drawable.bg_input_evohka)
        setPadding(dp(10), 0, dp(8), 0)
    }

    private fun crearInterfaz() {

        val margenHorizontal = resources.getDimensionPixelSize(
            R.dimen.evohka_screen_padding
        )

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                margenHorizontal,
                dp(16),
                margenHorizontal,
                dp(12)
            )
        }

        raiz.addView(
            contenido,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                weight = 1f
            }
        )

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
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }

        contenido.addView(
            titulo,
            parametrosTitulo
        )

        val panelFiltros = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_evohka)
            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )
        }

        panelFiltros.addView(TextView(this).apply {
            text = "Buscar y filtrar"
            textSize = 16f
            setTextColor(Color.parseColor("#211A24"))
            setTypeface(null, Typeface.BOLD)
        })

        etBuscarPropiedad = crearCampoFiltro(
            "Buscar por nombre, ciudad o barrio",
            InputType.TYPE_CLASS_TEXT
        ).apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }

        panelFiltros.addView(
            etBuscarPropiedad,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(10)
            }
        )

        spinnerCiudad = crearSelectorFiltro()
        spinnerTipoInmueble = crearSelectorFiltro()

        val filaSelectores = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                spinnerCiudad,
                LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginEnd = dp(5)
                }
            )
            addView(
                spinnerTipoInmueble,
                LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginStart = dp(5)
                }
            )
        }

        panelFiltros.addView(
            filaSelectores,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        etPrecioMinimo = crearCampoFiltro(
            "Precio mínimo",
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        etPrecioMaximo = crearCampoFiltro(
            "Precio máximo",
            InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        val filaPrecios = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                etPrecioMinimo,
                LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginEnd = dp(5)
                }
            )
            addView(
                etPrecioMaximo,
                LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginStart = dp(5)
                }
            )
        }

        panelFiltros.addView(
            filaPrecios,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        val btnLimpiarFiltros = Button(this).apply {
            text = "Limpiar filtros"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.parseColor("#211A24"))
            setBackgroundResource(R.drawable.bg_button_secondary_evohka)
            backgroundTintList = null
            setOnClickListener {
                limpiarFiltros()
            }
        }

        panelFiltros.addView(
            btnLimpiarFiltros,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                topMargin = dp(8)
            }
        )

        contenido.addView(
            panelFiltros,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        )

        configurarEventosFiltros()

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

        contenido.addView(
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

        contenido.addView(
            scrollView,
            parametrosScroll
        )

        contenido.addView(TextView(this).apply {
            text = "Accesos rápidos"
            textSize = 16f
            setTextColor(Color.parseColor("#211A24"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(10))
        })

        val accesos = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        listOf(
            "Fallas" to FallasActivity::class.java,
            "Chat" to ConversacionesActivity::class.java,
            "Ayuda" to FaqActivity::class.java
        ).forEachIndexed { indice, acceso ->
            accesos.addView(Button(this).apply {
                text = acceso.first
                isAllCaps = false
                textSize = 13f
                setTextColor(Color.parseColor("#211A24"))
                setBackgroundResource(R.drawable.bg_button_secondary_evohka)
                backgroundTintList = null
                setOnClickListener {
                    startActivity(Intent(this@PropiedadesDisponiblesActivity, acceso.second))
                }
            }, LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                if (indice > 0) marginStart = dp(6)
            })
        }

        contenido.addView(accesos, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        val btnCerrarSesion =
            Button(this).apply {

                text = "Cerrar sesión"
                isAllCaps = false
                textSize = 15f

                setTextColor(
                    Color.parseColor("#C62828")
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

        contenido.addView(
            btnCerrarSesion,
            parametrosCerrar
        )

        layoutInflater.inflate(
            R.layout.view_navegacion_arrendatario,
            raiz,
            true
        )

        setContentView(raiz)

        NavegacionEvohka.configurarArrendatario(
            this,
            NavegacionEvohka.DestinoArrendatario.INICIO
        )
    }

    private fun configurarEventosFiltros() {
        cargarOpcionesSelector(
            spinnerCiudad,
            "Todas las ciudades",
            emptyList()
        )
        cargarOpcionesSelector(
            spinnerTipoInmueble,
            "Todos los tipos",
            emptyList()
        )

        val observadorTexto = object : TextWatcher {
            override fun beforeTextChanged(
                texto: CharSequence?,
                inicio: Int,
                cantidad: Int,
                despues: Int
            ) = Unit

            override fun onTextChanged(
                texto: CharSequence?,
                inicio: Int,
                antes: Int,
                cantidad: Int
            ) = Unit

            override fun afterTextChanged(texto: Editable?) {
                if (!cambioProgramaticoFiltros) {
                    aplicarFiltros()
                }
            }
        }

        etBuscarPropiedad.addTextChangedListener(observadorTexto)
        etPrecioMinimo.addTextChangedListener(observadorTexto)
        etPrecioMaximo.addTextChangedListener(observadorTexto)

        val observadorSelector = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                padre: AdapterView<*>?,
                vista: android.view.View?,
                posicion: Int,
                id: Long
            ) {
                if (!cambioProgramaticoFiltros) {
                    aplicarFiltros()
                }
            }

            override fun onNothingSelected(padre: AdapterView<*>?) = Unit
        }

        spinnerCiudad.onItemSelectedListener = observadorSelector
        spinnerTipoInmueble.onItemSelectedListener = observadorSelector
    }

    private fun actualizarOpcionesFiltros(
        propiedades: List<Propiedad>
    ) {
        cambioProgramaticoFiltros = true
        try {
            cargarOpcionesSelector(
                spinnerCiudad,
                "Todas las ciudades",
                FiltroPropiedades.ciudadesDisponibles(propiedades)
            )
            cargarOpcionesSelector(
                spinnerTipoInmueble,
                "Todos los tipos",
                FiltroPropiedades.tiposDisponibles(propiedades)
            )
        } finally {
            cambioProgramaticoFiltros = false
        }
    }

    private fun cargarOpcionesSelector(
        selector: Spinner,
        opcionGeneral: String,
        opciones: List<String>
    ) {
        selector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(opcionGeneral) + opciones
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }
        selector.setSelection(0, false)
    }

    private fun limpiarFiltros() {
        cambioProgramaticoFiltros = true
        try {
            etBuscarPropiedad.text.clear()
            etPrecioMinimo.text.clear()
            etPrecioMaximo.text.clear()
            etPrecioMinimo.error = null
            etPrecioMaximo.error = null
            spinnerCiudad.setSelection(0, false)
            spinnerTipoInmueble.setSelection(0, false)
        } finally {
            cambioProgramaticoFiltros = false
        }
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        val token = tokenCatalogo ?: return
        val rango = FiltroPropiedades.validarRangoPrecio(
            etPrecioMinimo.text?.toString().orEmpty(),
            etPrecioMaximo.text?.toString().orEmpty()
        )

        etPrecioMinimo.error = null
        etPrecioMaximo.error = null
        if (!rango.esValido) {
            val mensaje = rango.mensajeError.orEmpty()
            contenedorPropiedades.removeAllViews()
            txtEstadoPropiedades.text = mensaje
            when {
                mensaje.contains("mínimo") && mensaje.contains("máximo") -> {
                    etPrecioMinimo.error = mensaje
                    etPrecioMaximo.error = mensaje
                }

                mensaje.contains("mínimo") -> etPrecioMinimo.error = mensaje
                else -> etPrecioMaximo.error = mensaje
            }
            return
        }

        val criterios = CriteriosFiltroPropiedades(
            texto = etBuscarPropiedad.text?.toString().orEmpty(),
            ciudad = seleccionFiltro(spinnerCiudad),
            tipoInmueble = seleccionFiltro(spinnerTipoInmueble),
            precioMinimo = rango.minimo,
            precioMaximo = rango.maximo
        )
        val propiedadesFiltradas = FiltroPropiedades.filtrar(
            propiedadesCargadas,
            criterios
        )

        mostrarPropiedades(
            propiedadesFiltradas,
            token,
            criterios.tieneFiltrosActivos()
        )
    }

    private fun seleccionFiltro(selector: Spinner): String? =
        selector.selectedItem
            ?.toString()
            ?.takeIf { selector.selectedItemPosition > 0 }

    private fun cargarPropiedades() {

        val token = SesionSegura.obtenerToken()

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

                            propiedadesCargadas = propiedades
                            tokenCatalogo = token
                            actualizarOpcionesFiltros(propiedades)
                            aplicarFiltros()

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
        propiedades: List<Propiedad>,
        token: String,
        filtrosActivos: Boolean
    ) {

        contenedorPropiedades
            .removeAllViews()

        if (propiedades.isEmpty()) {

            txtEstadoPropiedades.text =
                if (filtrosActivos && propiedadesCargadas.isNotEmpty()) {
                    "No encontramos inmuebles con estos filtros"
                } else {
                    "No hay propiedades disponibles"
                }

            return
        }

        txtEstadoPropiedades.text =
            if (propiedades.size == 1) {
                "1 propiedad encontrada"
            } else {
                "${propiedades.size} propiedades encontradas"
            }

        val formatoMoneda =
            NumberFormat.getCurrencyInstance(
                Locale.forLanguageTag("es-CO")
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

            val portada = FotografiasUi.imagenPortada(this)
            tarjeta.addView(portada)
            propiedad.id_propiedad
                ?.takeIf { it > 0 }
                ?.let {
                    FotografiasUi.cargarPortada(
                        portada,
                        it,
                        token
                    )
                }

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

            val canonValido = propiedad.canon
                ?.takeIf { it.isFinite() && it >= 0.0 }
            val canon = canonValido?.let { formatoMoneda.format(it) }
                ?: "Canon no disponible"

            val precio =
                TextView(this).apply {

                    text =
                        if (canonValido != null) {
                            "$canon / mes"
                        } else {
                            canon
                        }

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

        SesionSegura.cerrarSesion()
        NavegacionSesionInvalida.reiniciar()

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

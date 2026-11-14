package com.example.evohkaapp

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal object FotografiasUi {
    private fun dp(contexto: Context, valor: Int): Int =
        (valor * contexto.resources.displayMetrics.density).toInt()

    fun mostrarSeleccionadas(
        contexto: Context,
        contenedor: LinearLayout,
        uris: List<Uri>,
        habilitado: Boolean = true,
        alQuitar: (Uri) -> Unit
    ) {
        contenedor.removeAllViews()

        uris.forEach { uri ->
            val columna = LinearLayout(contexto).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, dp(contexto, 10), 0)
            }
            val miniatura = ImageView(contexto).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dp(contexto, 112),
                    dp(contexto, 86)
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_foto_neutra_evohka)
                clipToOutline = true
                contentDescription = "Fotografía seleccionada"
                cargarMiniatura(uri)
            }
            columna.addView(miniatura)

            val quitar = Button(contexto).apply {
                text = "Quitar"
                isAllCaps = false
                textSize = 12f
                isEnabled = habilitado
                setTextColor(Color.parseColor("#C62828"))
                setBackgroundResource(R.drawable.bg_button_outline_danger_evohka)
                backgroundTintList = null
                setOnClickListener { alQuitar(uri) }
            }
            columna.addView(
                quitar,
                LinearLayout.LayoutParams(
                    dp(contexto, 112),
                    dp(contexto, 40)
                ).apply { topMargin = dp(contexto, 5) }
            )
            contenedor.addView(columna)
        }
    }

    fun mostrarExistentesEditables(
        contexto: Context,
        contenedor: LinearLayout,
        fotografias: List<FotoPropiedad>,
        habilitado: Boolean,
        alDefinirPortada: (FotoPropiedad) -> Unit,
        alEliminar: (FotoPropiedad) -> Unit
    ) {
        contenedor.removeAllViews()

        fotografias.sortedBy { it.orden }.forEach { foto ->
            val columna = LinearLayout(contexto).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, dp(contexto, 10), 0)
            }
            val imagen = ImageView(contexto).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dp(contexto, 142),
                    dp(contexto, 100)
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_foto_neutra_evohka)
                clipToOutline = true
                contentDescription = if (foto.es_portada) {
                    "Fotografía de portada"
                } else {
                    "Fotografía del inmueble"
                }
                cargarFotografia(foto.ruta_http)
            }
            columna.addView(imagen)

            val estado = TextView(contexto).apply {
                text = if (foto.es_portada) "Portada" else "Fotografía"
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(
                    Color.parseColor(if (foto.es_portada) "#7B2FF7" else "#6E6672")
                )
                if (foto.es_portada) setTypeface(null, Typeface.BOLD)
            }
            columna.addView(
                estado,
                LinearLayout.LayoutParams(
                    dp(contexto, 142),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(contexto, 4) }
            )

            if (!foto.es_portada) {
                columna.addView(
                    Button(contexto).apply {
                        text = "Usar de portada"
                        isAllCaps = false
                        textSize = 11f
                        isEnabled = habilitado
                        setBackgroundResource(R.drawable.bg_button_secondary_evohka)
                        backgroundTintList = null
                        setOnClickListener { alDefinirPortada(foto) }
                    },
                    LinearLayout.LayoutParams(
                        dp(contexto, 142),
                        dp(contexto, 40)
                    ).apply { topMargin = dp(contexto, 4) }
                )
            }

            columna.addView(
                Button(contexto).apply {
                    text = "Eliminar"
                    isAllCaps = false
                    textSize = 11f
                    isEnabled = habilitado
                    setTextColor(Color.parseColor("#C62828"))
                    setBackgroundResource(R.drawable.bg_button_outline_danger_evohka)
                    backgroundTintList = null
                    setOnClickListener { alEliminar(foto) }
                },
                LinearLayout.LayoutParams(
                    dp(contexto, 142),
                    dp(contexto, 40)
                ).apply { topMargin = dp(contexto, 4) }
            )

            contenedor.addView(columna)
        }
    }

    fun mostrarGaleria(
        contexto: Context,
        contenedor: LinearLayout,
        fotografias: List<FotoPropiedad>
    ) {
        contenedor.removeAllViews()
        if (fotografias.isEmpty()) {
            contenedor.addView(imagenNeutra(contexto, 280, 180))
            return
        }

        fotografias.sortedBy { it.orden }.forEach { foto ->
            val imagen = ImageView(contexto).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_foto_neutra_evohka)
                clipToOutline = true
                contentDescription = if (foto.es_portada) {
                    "Portada del inmueble"
                } else {
                    "Fotografía del inmueble"
                }
                cargarFotografia(foto.ruta_http)
            }
            contenedor.addView(
                imagen,
                LinearLayout.LayoutParams(
                    dp(contexto, 280),
                    dp(contexto, 180)
                ).apply { marginEnd = dp(contexto, 10) }
            )
        }
    }

    fun imagenPortada(contexto: Context): ImageView =
        imagenNeutra(contexto, 0, 170).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(contexto, 170)
            ).apply { bottomMargin = dp(contexto, 12) }
        }

    fun cargarPortada(
        imageView: ImageView,
        idPropiedad: Int,
        token: String
    ) {
        imageView.tag = idPropiedad
        imageView.setImageResource(R.drawable.ic_foto_neutra_evohka)

        RetrofitClient.apiService
            .listarFotografiasPropiedad(token, idPropiedad)
            .enqueue(object : Callback<RespuestaFotosPropiedad> {
                override fun onResponse(
                    call: Call<RespuestaFotosPropiedad>,
                    response: Response<RespuestaFotosPropiedad>
                ) {
                    if (imageView.tag != idPropiedad || !response.isSuccessful) {
                        return
                    }
                    val fotografias = response.body()?.datos.orEmpty()
                    val portada = fotografias.firstOrNull { it.es_portada }
                        ?: fotografias.minByOrNull { it.orden }
                    imageView.cargarFotografia(portada?.ruta_http)
                }

                override fun onFailure(
                    call: Call<RespuestaFotosPropiedad>,
                    t: Throwable
                ) {
                    // Se conserva la imagen neutra; no se interrumpe el listado.
                }
            })
    }

    private fun imagenNeutra(
        contexto: Context,
        anchoDp: Int,
        altoDp: Int
    ): ImageView = ImageView(contexto).apply {
        if (anchoDp > 0) {
            layoutParams = LinearLayout.LayoutParams(
                dp(contexto, anchoDp),
                dp(contexto, altoDp)
            )
        }
        scaleType = ImageView.ScaleType.CENTER
        setImageResource(R.drawable.ic_foto_neutra_evohka)
        setBackgroundResource(R.drawable.bg_foto_neutra_evohka)
        clipToOutline = true
        contentDescription = "Inmueble sin fotografías"
    }
}

package com.example.evohkaapp

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PublicarInmuebleActivity : EvohkaActivity() {
    private val fotografiasSeleccionadas = mutableListOf<Uri>()
    private var idPropiedadPendienteFotos: Int? = null
    private var operacionEnCurso = false

    private lateinit var btnGuardar: Button
    private lateinit var btnAnadirFotos: Button
    private lateinit var txtContadorFotos: TextView
    private lateinit var contenedorFotos: LinearLayout

    private val selectorFotos = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            PoliticaFotografiasAndroid.MAX_FOTOGRAFIAS
        )
    ) { uris ->
        val resultado = FotografiaMultipart.validarSeleccion(
            this,
            uris,
            fotografiasSeleccionadas
        )
        fotografiasSeleccionadas += resultado.aceptadas
        actualizarSeleccionFotos()
        if (resultado.descartadas > 0) {
            Toast.makeText(
                this,
                resultado.ultimoMotivo
                    ?: "Algunas fotografías no pudieron seleccionarse",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicar_inmueble)

        val etTitulo = findViewById<EditText>(R.id.et_titulo)
        val etTipoInmueble = findViewById<EditText>(R.id.et_tipo_inmueble)
        val etDireccion = findViewById<EditText>(R.id.et_direccion)
        val etBarrio = findViewById<EditText>(R.id.et_barrio)
        val etCiudad = findViewById<EditText>(R.id.et_ciudad)
        val etCanon = findViewById<EditText>(R.id.et_canon)
        val etHabitaciones = findViewById<EditText>(R.id.et_habitaciones)
        val etBanos = findViewById<EditText>(R.id.et_banos)

        btnGuardar = findViewById(R.id.btn_guardar_inmueble)
        btnAnadirFotos = findViewById(R.id.btn_anadir_fotos_publicar)
        txtContadorFotos = findViewById(R.id.txt_contador_fotos_publicar)
        contenedorFotos = findViewById(R.id.contenedor_fotos_publicar)
        val btnCancelar = findViewById<Button>(R.id.btn_cancelar)

        savedInstanceState
            ?.getStringArrayList(ESTADO_FOTOS)
            ?.mapTo(fotografiasSeleccionadas) { Uri.parse(it) }
        idPropiedadPendienteFotos = savedInstanceState
            ?.getInt(ESTADO_ID_PROPIEDAD, 0)
            ?.takeIf { it > 0 }

        actualizarSeleccionFotos()

        btnAnadirFotos.setOnClickListener {
            selectorFotos.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        btnCancelar.setOnClickListener { finish() }

        btnGuardar.setOnClickListener {
            if (operacionEnCurso) return@setOnClickListener

            val pendiente = idPropiedadPendienteFotos
            if (pendiente != null) {
                if (fotografiasSeleccionadas.isEmpty()) {
                    Toast.makeText(
                        this,
                        "El inmueble quedó publicado sin fotografías",
                        Toast.LENGTH_LONG
                    ).show()
                    resolverDestinoPostGuardado(
                        fotografiasPendientes = false,
                        cargaFotografiasExitosa = false
                    )
                } else {
                    subirFotografias(pendiente)
                }
                return@setOnClickListener
            }

            val propiedad = validarPropiedad(
                etTitulo,
                etTipoInmueble,
                etDireccion,
                etBarrio,
                etCiudad,
                etCanon,
                etHabitaciones,
                etBanos
            ) ?: return@setOnClickListener

            registrarPropiedad(propiedad)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(
            ESTADO_FOTOS,
            ArrayList(fotografiasSeleccionadas.map(Uri::toString))
        )
        idPropiedadPendienteFotos?.let {
            outState.putInt(ESTADO_ID_PROPIEDAD, it)
        }
    }

    private fun validarPropiedad(
        etTitulo: EditText,
        etTipoInmueble: EditText,
        etDireccion: EditText,
        etBarrio: EditText,
        etCiudad: EditText,
        etCanon: EditText,
        etHabitaciones: EditText,
        etBanos: EditText
    ): PropiedadRequest? {
        val textos = listOf(
            etTitulo,
            etTipoInmueble,
            etDireccion,
            etBarrio,
            etCiudad
        ).map { it.text.toString().trim() }
        val canonTexto = etCanon.text.toString().trim()
        val habitacionesTexto = etHabitaciones.text.toString().trim()
        val banosTexto = etBanos.text.toString().trim()

        if (textos.any(String::isEmpty) ||
            canonTexto.isEmpty() ||
            habitacionesTexto.isEmpty() ||
            banosTexto.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Completa todos los campos",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        val canon = canonTexto.toDoubleOrNull()
        val habitaciones = habitacionesTexto.toIntOrNull()
        val banos = banosTexto.toIntOrNull()
        if (canon == null || canon <= 0) {
            Toast.makeText(
                this,
                "Ingresa un canon mensual válido",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }
        if (habitaciones == null || habitaciones < 0) {
            Toast.makeText(
                this,
                "Ingresa una cantidad válida de habitaciones",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }
        if (banos == null || banos < 0) {
            Toast.makeText(
                this,
                "Ingresa una cantidad válida de baños",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        return PropiedadRequest(
            titulo = textos[0],
            tipo_inmueble = textos[1],
            direccion = textos[2],
            barrio = textos[3],
            ciudad = textos[4],
            canon = canon,
            habitaciones = habitaciones,
            banos = banos
        )
    }

    private fun registrarPropiedad(propiedad: PropiedadRequest) {
        val token = tokenActual() ?: return
        cambiarEstadoOperacion(true, "Publicando...")

        RetrofitClient.apiService
            .registrarPropiedad(token, propiedad)
            .enqueue(object : Callback<RespuestaPropiedad> {
                override fun onResponse(
                    call: Call<RespuestaPropiedad>,
                    response: Response<RespuestaPropiedad>
                ) {
                    if (!response.isSuccessful) {
                        cambiarEstadoOperacion(false, "Publicar inmueble")
                        Toast.makeText(
                            this@PublicarInmuebleActivity,
                            mensajeError(
                                response,
                                "No se pudo registrar la propiedad"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val idPropiedad = response.body()?.datos?.id_propiedad
                    if (idPropiedad == null || idPropiedad <= 0) {
                        cambiarEstadoOperacion(false, "Publicar inmueble")
                        Toast.makeText(
                            this@PublicarInmuebleActivity,
                            "El inmueble se publicó, pero no fue posible asociar fotografías",
                            Toast.LENGTH_LONG
                        ).show()
                        cambiarEstadoOperacion(true, "Inmueble publicado")
                        AlertDialog.Builder(this@PublicarInmuebleActivity)
                            .setTitle("Inmueble publicado")
                            .setMessage(
                                "La respuesta no incluyó el identificador necesario para asociar las fotografías. " +
                                    "Revisa el inmueble publicado antes de volver a intentarlo."
                            )
                            .setPositiveButton("Ir a Mis inmuebles") { _, _ ->
                                regresarAMisInmuebles()
                            }
                            .show()
                        return
                    }

                    idPropiedadPendienteFotos = idPropiedad
                    if (fotografiasSeleccionadas.isEmpty()) {
                        Toast.makeText(
                            this@PublicarInmuebleActivity,
                            "Propiedad publicada correctamente",
                            Toast.LENGTH_LONG
                        ).show()
                        resolverDestinoPostGuardado(
                            fotografiasPendientes = false,
                            cargaFotografiasExitosa = false
                        )
                    } else {
                        subirFotografias(idPropiedad)
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaPropiedad>,
                    t: Throwable
                ) {
                    cambiarEstadoOperacion(false, "Publicar inmueble")
                    Toast.makeText(
                        this@PublicarInmuebleActivity,
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun subirFotografias(idPropiedad: Int) {
        val token = tokenActual() ?: return
        val partes = runCatching {
            FotografiaMultipart.crearPartes(this, fotografiasSeleccionadas)
        }.getOrElse {
            cambiarEstadoOperacion(false, "Reintentar fotografías")
            Toast.makeText(
                this,
                "No fue posible abrir una fotografía seleccionada",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        cambiarEstadoOperacion(true, "Subiendo fotografías...")
        RetrofitClient.apiService
            .subirFotografiasPropiedad(token, idPropiedad, partes)
            .enqueue(object : Callback<RespuestaFotosPropiedad> {
                override fun onResponse(
                    call: Call<RespuestaFotosPropiedad>,
                    response: Response<RespuestaFotosPropiedad>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@PublicarInmuebleActivity,
                            "Propiedad y fotografías publicadas correctamente",
                            Toast.LENGTH_LONG
                        ).show()
                        resolverDestinoPostGuardado(
                            fotografiasPendientes = true,
                            cargaFotografiasExitosa = true
                        )
                    } else {
                        mostrarFalloFotografias(
                            mensajeError(
                                response,
                                "El inmueble se publicó, pero las fotografías no se cargaron"
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaFotosPropiedad>,
                    t: Throwable
                ) {
                    mostrarFalloFotografias(
                        "El inmueble se publicó, pero no fue posible conectar para subir las fotografías"
                    )
                }
            })
    }

    private fun mostrarFalloFotografias(mensaje: String) {
        cambiarEstadoOperacion(false, "Reintentar fotografías")
        AlertDialog.Builder(this)
            .setTitle("Inmueble publicado")
            .setMessage("$mensaje. Puedes reintentar sin crear otro inmueble.")
            .setNegativeButton("Permanecer aquí", null)
            .setPositiveButton("Reintentar") { _, _ ->
                idPropiedadPendienteFotos?.let(::subirFotografias)
            }
            .show()
    }

    private fun resolverDestinoPostGuardado(
        fotografiasPendientes: Boolean,
        cargaFotografiasExitosa: Boolean
    ) {
        if (
            PoliticaNavegacionFotografias.decidir(
                fotografiasPendientes,
                cargaFotografiasExitosa
            ) == DestinoPostGuardadoFotografias.MIS_INMUEBLES
        ) {
            regresarAMisInmuebles()
        }
    }

    private fun regresarAMisInmuebles() {
        startActivity(
            Intent(this, MisInmueblesActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
    }

    private fun actualizarSeleccionFotos() {
        txtContadorFotos.text =
            "Fotografías (${fotografiasSeleccionadas.size}/10)"
        btnAnadirFotos.isEnabled =
            !operacionEnCurso &&
                fotografiasSeleccionadas.size <
                PoliticaFotografiasAndroid.MAX_FOTOGRAFIAS
        FotografiasUi.mostrarSeleccionadas(
            this,
            contenedorFotos,
            fotografiasSeleccionadas,
            !operacionEnCurso
        ) { uri ->
            fotografiasSeleccionadas.remove(uri)
            actualizarSeleccionFotos()
        }
    }

    private fun cambiarEstadoOperacion(enCurso: Boolean, texto: String) {
        operacionEnCurso = enCurso
        btnGuardar.isEnabled = !enCurso
        btnGuardar.text = texto
        actualizarSeleccionFotos()
    }

    private fun tokenActual(): String? {
        val token = SesionSegura.obtenerToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "La sesión ha finalizado. Inicia sesión nuevamente.",
                Toast.LENGTH_LONG
            ).show()
        }
        return token
    }

    private fun mensajeError(
        response: Response<*>,
        predeterminado: String
    ): String = try {
        val contenido = response.errorBody()?.string()
        if (contenido.isNullOrBlank()) predeterminado else {
            JSONObject(contenido).optString("mensaje", predeterminado)
        }
    } catch (_: Exception) {
        predeterminado
    }

    private companion object {
        const val ESTADO_FOTOS = "fotos_seleccionadas"
        const val ESTADO_ID_PROPIEDAD = "id_propiedad_pendiente_fotos"
    }
}

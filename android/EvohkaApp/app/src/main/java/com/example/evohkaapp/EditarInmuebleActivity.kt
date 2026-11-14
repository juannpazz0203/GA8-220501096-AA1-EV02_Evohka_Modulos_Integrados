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

class EditarInmuebleActivity : EvohkaActivity() {
    private var idPropiedad: Int = 0
    private var operacionEnCurso = false
    private var cargandoFotografias = true
    private var fotosPendientesTrasActualizacion = false
    private val fotografiasExistentes = mutableListOf<FotoPropiedad>()
    private val fotografiasNuevas = mutableListOf<Uri>()

    private lateinit var btnActualizar: Button
    private lateinit var btnAnadirFotos: Button
    private lateinit var txtEstadoFotos: TextView
    private lateinit var contenedorExistentes: LinearLayout
    private lateinit var contenedorNuevas: LinearLayout

    private val selectorFotos = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            PoliticaFotografiasAndroid.MAX_FOTOGRAFIAS
        )
    ) { uris ->
        val resultado = FotografiaMultipart.validarSeleccion(
            this,
            uris,
            fotografiasNuevas,
            fotografiasExistentes.size
        )
        fotografiasNuevas += resultado.aceptadas
        actualizarGalerias()
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
        setContentView(R.layout.activity_editar_inmueble)

        idPropiedad = intent.getIntExtra("id_propiedad", 0)
        if (idPropiedad <= 0) {
            Toast.makeText(
                this,
                "No se pudo identificar la propiedad",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val etTitulo = findViewById<EditText>(R.id.et_titulo)
        val etTipoInmueble = findViewById<EditText>(R.id.et_tipo_inmueble)
        val etDireccion = findViewById<EditText>(R.id.et_direccion)
        val etBarrio = findViewById<EditText>(R.id.et_barrio)
        val etCiudad = findViewById<EditText>(R.id.et_ciudad)
        val etCanon = findViewById<EditText>(R.id.et_canon)
        val etHabitaciones = findViewById<EditText>(R.id.et_habitaciones)
        val etBanos = findViewById<EditText>(R.id.et_banos)

        btnActualizar = findViewById(R.id.btn_actualizar_inmueble)
        btnAnadirFotos = findViewById(R.id.btn_anadir_fotos_editar)
        txtEstadoFotos = findViewById(R.id.txt_estado_fotos_editar)
        contenedorExistentes = findViewById(R.id.contenedor_fotos_existentes)
        contenedorNuevas = findViewById(R.id.contenedor_fotos_nuevas)
        val btnCancelar = findViewById<Button>(R.id.btn_cancelar_edicion)

        etTitulo.setText(intent.getStringExtra("titulo").orEmpty())
        etTipoInmueble.setText(intent.getStringExtra("tipo_inmueble").orEmpty())
        etDireccion.setText(intent.getStringExtra("direccion").orEmpty())
        etBarrio.setText(intent.getStringExtra("barrio").orEmpty())
        etCiudad.setText(intent.getStringExtra("ciudad").orEmpty())
        etCanon.setText(intent.getDoubleExtra("canon", 0.0).toString())
        etHabitaciones.setText(
            intent.getIntExtra("habitaciones", 0).toString()
        )
        etBanos.setText(intent.getIntExtra("banos", 0).toString())

        savedInstanceState
            ?.getStringArrayList(ESTADO_FOTOS_NUEVAS)
            ?.mapTo(fotografiasNuevas) { Uri.parse(it) }
        fotosPendientesTrasActualizacion = savedInstanceState
            ?.getBoolean(ESTADO_FOTOS_PENDIENTES, false)
            ?: false

        btnAnadirFotos.setOnClickListener {
            selectorFotos.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
        btnCancelar.setOnClickListener { finish() }
        btnActualizar.setOnClickListener {
            if (operacionEnCurso) return@setOnClickListener
            if (fotosPendientesTrasActualizacion && fotografiasNuevas.isNotEmpty()) {
                tokenActual()?.let(::subirFotografiasNuevas)
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
            actualizarPropiedad(propiedad)
        }

        actualizarGalerias()
        cargarFotografias()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(
            ESTADO_FOTOS_NUEVAS,
            ArrayList(fotografiasNuevas.map(Uri::toString))
        )
        outState.putBoolean(
            ESTADO_FOTOS_PENDIENTES,
            fotosPendientesTrasActualizacion
        )
    }

    private fun cargarFotografias() {
        val token = tokenActual() ?: return
        cargandoFotografias = true
        actualizarGalerias()

        RetrofitClient.apiService
            .listarFotografiasPropiedad(token, idPropiedad)
            .enqueue(object : Callback<RespuestaFotosPropiedad> {
                override fun onResponse(
                    call: Call<RespuestaFotosPropiedad>,
                    response: Response<RespuestaFotosPropiedad>
                ) {
                    cargandoFotografias = false
                    if (response.isSuccessful) {
                        fotografiasExistentes.clear()
                        fotografiasExistentes += response.body()?.datos.orEmpty()
                    } else {
                        Toast.makeText(
                            this@EditarInmuebleActivity,
                            mensajeError(
                                response,
                                "No se pudieron consultar las fotografías"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    actualizarGalerias()
                }

                override fun onFailure(
                    call: Call<RespuestaFotosPropiedad>,
                    t: Throwable
                ) {
                    cargandoFotografias = false
                    actualizarGalerias()
                    Toast.makeText(
                        this@EditarInmuebleActivity,
                        "No se pudieron consultar las fotografías",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun actualizarPropiedad(propiedad: PropiedadRequest) {
        val token = tokenActual() ?: return
        cambiarEstadoOperacion(true, "Guardando...")

        RetrofitClient.apiService
            .actualizarPropiedad(token, idPropiedad, propiedad)
            .enqueue(object : Callback<RespuestaPropiedad> {
                override fun onResponse(
                    call: Call<RespuestaPropiedad>,
                    response: Response<RespuestaPropiedad>
                ) {
                    if (!response.isSuccessful) {
                        cambiarEstadoOperacion(false, "Guardar cambios")
                        Toast.makeText(
                            this@EditarInmuebleActivity,
                            mensajeError(
                                response,
                                "No se pudo actualizar la propiedad"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    if (fotografiasNuevas.isEmpty()) {
                        Toast.makeText(
                            this@EditarInmuebleActivity,
                            response.body()?.mensaje
                                ?: "Propiedad actualizada correctamente",
                            Toast.LENGTH_LONG
                        ).show()
                        resolverDestinoPostGuardado(
                            fotografiasPendientes = false,
                            cargaFotografiasExitosa = false
                        )
                    } else {
                        fotosPendientesTrasActualizacion = true
                        subirFotografiasNuevas(token)
                    }
                }

                override fun onFailure(call: Call<RespuestaPropiedad>, t: Throwable) {
                    cambiarEstadoOperacion(false, "Guardar cambios")
                    Toast.makeText(
                        this@EditarInmuebleActivity,
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun subirFotografiasNuevas(token: String) {
        val partes = runCatching {
            FotografiaMultipart.crearPartes(this, fotografiasNuevas)
        }.getOrElse {
            cambiarEstadoOperacion(false, "Reintentar fotografías")
            Toast.makeText(
                this,
                "La propiedad se actualizó, pero no fue posible abrir una fotografía",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnActualizar.text = "Subiendo fotografías..."
        RetrofitClient.apiService
            .subirFotografiasPropiedad(token, idPropiedad, partes)
            .enqueue(object : Callback<RespuestaFotosPropiedad> {
                override fun onResponse(
                    call: Call<RespuestaFotosPropiedad>,
                    response: Response<RespuestaFotosPropiedad>
                ) {
                    if (response.isSuccessful) {
                        fotografiasNuevas.clear()
                        fotosPendientesTrasActualizacion = false
                        cambiarEstadoOperacion(false, "Guardar cambios")
                        Toast.makeText(
                            this@EditarInmuebleActivity,
                            "Cambios y fotografías guardados correctamente",
                            Toast.LENGTH_LONG
                        ).show()
                        resolverDestinoPostGuardado(
                            fotografiasPendientes = true,
                            cargaFotografiasExitosa = true
                        )
                    } else {
                        cambiarEstadoOperacion(false, "Reintentar fotografías")
                        Toast.makeText(
                            this@EditarInmuebleActivity,
                            mensajeError(
                                response,
                                "La propiedad se actualizó, pero las fotografías no se cargaron"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaFotosPropiedad>,
                    t: Throwable
                ) {
                    cambiarEstadoOperacion(false, "Reintentar fotografías")
                    Toast.makeText(
                        this@EditarInmuebleActivity,
                        "La propiedad se actualizó. Reintenta la carga de fotografías.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
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

    private fun confirmarEliminarFotografia(foto: FotoPropiedad) {
        if (operacionEnCurso) return
        AlertDialog.Builder(this)
            .setTitle("Eliminar fotografía")
            .setMessage("¿Deseas eliminar esta fotografía del inmueble?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ -> eliminarFotografia(foto) }
            .show()
    }

    private fun eliminarFotografia(foto: FotoPropiedad) {
        val token = tokenActual() ?: return
        cambiarEstadoOperacion(true, "Actualizando fotografías...")
        RetrofitClient.apiService
            .eliminarFotografiaPropiedad(token, idPropiedad, foto.id_foto)
            .enqueue(object : Callback<RespuestaFotoPropiedad> {
                override fun onResponse(
                    call: Call<RespuestaFotoPropiedad>,
                    response: Response<RespuestaFotoPropiedad>
                ) {
                    cambiarEstadoOperacion(false, "Guardar cambios")
                    if (response.isSuccessful) {
                        cargarFotografias()
                    } else {
                        Toast.makeText(
                            this@EditarInmuebleActivity,
                            mensajeError(response, "No se pudo eliminar la fotografía"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaFotoPropiedad>,
                    t: Throwable
                ) {
                    cambiarEstadoOperacion(false, "Guardar cambios")
                    Toast.makeText(
                        this@EditarInmuebleActivity,
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun definirPortada(foto: FotoPropiedad) {
        if (operacionEnCurso || foto.es_portada) return
        val token = tokenActual() ?: return
        val ids = fotografiasExistentes.sortedBy { it.orden }.map { it.id_foto }
        cambiarEstadoOperacion(true, "Actualizando portada...")

        RetrofitClient.apiService
            .reordenarFotografiasPropiedad(
                token,
                idPropiedad,
                ReordenarFotosRequest(ids, foto.id_foto)
            )
            .enqueue(object : Callback<RespuestaFotosPropiedad> {
                override fun onResponse(
                    call: Call<RespuestaFotosPropiedad>,
                    response: Response<RespuestaFotosPropiedad>
                ) {
                    cambiarEstadoOperacion(false, "Guardar cambios")
                    if (response.isSuccessful) {
                        fotografiasExistentes.clear()
                        fotografiasExistentes += response.body()?.datos.orEmpty()
                        actualizarGalerias()
                    } else {
                        Toast.makeText(
                            this@EditarInmuebleActivity,
                            mensajeError(response, "No se pudo cambiar la portada"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaFotosPropiedad>,
                    t: Throwable
                ) {
                    cambiarEstadoOperacion(false, "Guardar cambios")
                    Toast.makeText(
                        this@EditarInmuebleActivity,
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun actualizarGalerias() {
        val total = fotografiasExistentes.size + fotografiasNuevas.size
        txtEstadoFotos.text = when {
            cargandoFotografias -> "Fotografías (consultando...)"
            total == 0 -> "Fotografías (0/10) · Sin fotografías"
            else -> "Fotografías ($total/10)"
        }
        btnAnadirFotos.isEnabled =
            !operacionEnCurso &&
                !cargandoFotografias &&
                total < PoliticaFotografiasAndroid.MAX_FOTOGRAFIAS

        FotografiasUi.mostrarExistentesEditables(
            this,
            contenedorExistentes,
            fotografiasExistentes,
            !operacionEnCurso,
            ::definirPortada,
            ::confirmarEliminarFotografia
        )
        FotografiasUi.mostrarSeleccionadas(
            this,
            contenedorNuevas,
            fotografiasNuevas,
            !operacionEnCurso
        ) { uri ->
            fotografiasNuevas.remove(uri)
            if (fotografiasNuevas.isEmpty() && fotosPendientesTrasActualizacion) {
                fotosPendientesTrasActualizacion = false
                btnActualizar.text = "Guardar cambios"
            }
            actualizarGalerias()
        }
    }

    private fun cambiarEstadoOperacion(enCurso: Boolean, textoBoton: String) {
        operacionEnCurso = enCurso
        btnActualizar.isEnabled = !enCurso
        btnActualizar.text = textoBoton
        actualizarGalerias()
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
        val canon = etCanon.text.toString().trim().toDoubleOrNull()
        val habitaciones = etHabitaciones.text.toString().trim().toIntOrNull()
        val banos = etBanos.text.toString().trim().toIntOrNull()

        if (textos.any(String::isEmpty) || canon == null || canon <= 0 ||
            habitaciones == null || habitaciones < 0 ||
            banos == null || banos < 0
        ) {
            Toast.makeText(
                this,
                "Completa todos los campos con valores válidos",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        return PropiedadRequest(
            textos[0],
            textos[1],
            textos[2],
            textos[3],
            textos[4],
            canon,
            habitaciones,
            banos
        )
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
        const val ESTADO_FOTOS_NUEVAS = "fotos_nuevas"
        const val ESTADO_FOTOS_PENDIENTES = "fotos_pendientes_tras_actualizacion"
    }
}

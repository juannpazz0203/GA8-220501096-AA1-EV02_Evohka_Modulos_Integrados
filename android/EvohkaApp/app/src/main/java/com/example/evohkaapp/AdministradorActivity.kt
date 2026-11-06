package com.example.evohkaapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
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

    private fun token(): String? {
        return getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
            .getString("token", null)
    }

    private fun cargarResumen() {
        val sesion = token()
        if (sesion.isNullOrBlank()) {
            txtEstado.text = "Sesión no válida"
            return
        }

        txtEstado.text = "Consultando información..."
        RetrofitClient.apiService.obtenerResumenAdministrador(sesion).enqueue(
            object : Callback<RespuestaAdmin> {
                override fun onResponse(
                    call: Call<RespuestaAdmin>,
                    response: Response<RespuestaAdmin>
                ) {
                    val datos = response.body()?.datos
                    if (response.isSuccessful && datos != null) {
                        cargarUsuarios(sesion, datos)
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

    private fun cargarUsuarios(sesion: String, resumen: AdminResumen) {
        RetrofitClient.apiService.listarUsuariosAdministrador(sesion).enqueue(
            object : Callback<RespuestaUsuariosAdmin> {
                override fun onResponse(
                    call: Call<RespuestaUsuariosAdmin>,
                    response: Response<RespuestaUsuariosAdmin>
                ) {
                    if (response.isSuccessful) {
                        mostrar(resumen, response.body()?.datos.orEmpty())
                    } else {
                        mostrar(resumen, resumen.usuarios)
                        txtEstado.text = mensajeError(
                            response,
                            "No se pudo actualizar la lista completa de usuarios"
                        )
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaUsuariosAdmin>,
                    t: Throwable
                ) {
                    mostrar(resumen, resumen.usuarios)
                    txtEstado.text = "Resumen disponible · no se pudo actualizar usuarios"
                }
            }
        )
    }

    private fun mostrar(datos: AdminResumen, usuarios: List<AdminUsuario>) {
        contenedor.removeAllViews()
        txtEstado.text = "${usuarios.size} usuario(s) · demás secciones de consulta"
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
        seccionUsuarios(usuarios)
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

    private fun seccionUsuarios(usuarios: List<AdminUsuario>) {
        val bloque = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundResource(R.drawable.bg_card_evohka)
        }
        val cabecera = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cabecera.addView(TextView(this).apply {
            text = "Usuarios"
            textSize = 18f
            setTextColor(Color.parseColor("#7B2FF7"))
            setTypeface(null, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        cabecera.addView(Button(this).apply {
            text = "Crear usuario"
            isAllCaps = false
            setTextColor(Color.parseColor("#211A24"))
            setBackgroundResource(R.drawable.bg_button_evohka)
            backgroundTintList = null
            setOnClickListener { mostrarFormularioUsuario(null) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)))
        bloque.addView(cabecera)

        if (usuarios.isEmpty()) {
            bloque.addView(fila("Sin usuarios"))
        } else {
            usuarios.forEach { usuario -> bloque.addView(tarjetaUsuario(usuario)) }
        }

        contenedor.addView(
            bloque,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        )
    }

    private fun tarjetaUsuario(usuario: AdminUsuario): LinearLayout {
        val actual = getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
            .getInt("id_usuario", 0) == usuario.id_usuario
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundResource(R.drawable.bg_button_secondary_evohka)

            addView(TextView(this@AdministradorActivity).apply {
                val nombreCompleto = "${usuario.nombre} ${usuario.apellido.orEmpty()}".trim()
                text = "#${usuario.id_usuario} $nombreCompleto${if (actual) " · Sesión actual" else ""}"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#211A24"))
            })
            addView(TextView(this@AdministradorActivity).apply {
                text = "${usuario.correo} · ${usuario.rol}"
                textSize = 13f
                setTextColor(Color.parseColor("#5F5862"))
                setPadding(0, dp(4), 0, dp(8))
            })

            val acciones = LinearLayout(this@AdministradorActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            acciones.addView(Button(this@AdministradorActivity).apply {
                text = "Editar"
                isAllCaps = false
                setBackgroundResource(R.drawable.bg_button_secondary_evohka)
                backgroundTintList = null
                setOnClickListener { mostrarFormularioUsuario(usuario) }
            }, LinearLayout.LayoutParams(0, dp(42), 1f))
            acciones.addView(Button(this@AdministradorActivity).apply {
                text = "Eliminar"
                isAllCaps = false
                setTextColor(Color.parseColor("#C62828"))
                setBackgroundResource(R.drawable.bg_button_outline_danger_evohka)
                backgroundTintList = null
                setOnClickListener { confirmarEliminacion(usuario) }
            }, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                marginStart = dp(8)
            })
            addView(acciones)
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
    }

    private fun mostrarFormularioUsuario(usuario: AdminUsuario?) {
        val esCreacion = usuario == null
        val formulario = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
        }
        val nombre = campo("Nombre", InputType.TYPE_CLASS_TEXT).apply {
            setText(usuario?.nombre.orEmpty())
        }
        val apellido = campo("Apellido", InputType.TYPE_CLASS_TEXT).apply {
            setText(usuario?.apellido.orEmpty())
        }
        val correo = campo(
            "Correo electrónico",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        ).apply { setText(usuario?.correo.orEmpty()) }
        val telefono = campo("Teléfono (opcional)", InputType.TYPE_CLASS_PHONE).apply {
            setText(usuario?.telefono.orEmpty())
        }
        val contrasena = if (esCreacion) {
            campo(
                "Contraseña (mínimo 6 caracteres)",
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            )
        } else null
        val roles = listOf("arrendatario", "propietario", "administrador")
        val selectorRol = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AdministradorActivity,
                android.R.layout.simple_spinner_item,
                roles
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(roles.indexOf(usuario?.rol).coerceAtLeast(0))
        }

        formulario.addView(nombre)
        formulario.addView(apellido)
        formulario.addView(correo)
        formulario.addView(telefono)
        if (contrasena != null) formulario.addView(contrasena)
        formulario.addView(TextView(this).apply {
            text = "Rol"
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#211A24"))
            setPadding(0, dp(12), 0, dp(4))
        })
        formulario.addView(selectorRol)

        AlertDialog.Builder(this)
            .setTitle(if (esCreacion) "Crear usuario" else "Editar usuario")
            .setView(formulario)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(if (esCreacion) "Crear" else "Guardar") { _, _ ->
                val nombreValor = nombre.text.toString().trim()
                val apellidoValor = apellido.text.toString().trim()
                val correoValor = correo.text.toString().trim()
                val telefonoValor = telefono.text.toString().trim().ifBlank { null }
                val rolValor = selectorRol.selectedItem.toString()
                if (nombreValor.isBlank() || apellidoValor.isBlank() || correoValor.isBlank()) {
                    Toast.makeText(
                        this,
                        "Nombre, apellido y correo son obligatorios",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (esCreacion) {
                    val clave = contrasena?.text?.toString().orEmpty()
                    if (clave.length < 6) {
                        Toast.makeText(this, "La contraseña debe tener mínimo 6 caracteres", Toast.LENGTH_LONG).show()
                    } else {
                        crearUsuario(
                            nombreValor,
                            apellidoValor,
                            correoValor,
                            clave,
                            telefonoValor,
                            rolValor
                        )
                    }
                } else {
                    actualizarUsuario(
                        usuario!!.id_usuario,
                        nombreValor,
                        apellidoValor,
                        correoValor,
                        telefonoValor,
                        rolValor
                    )
                }
            }
            .show()
    }

    private fun campo(hint: String, tipo: Int): EditText {
        return EditText(this).apply {
            this.hint = hint
            inputType = tipo
            setSingleLine(true)
            setBackgroundResource(R.drawable.bg_input_evohka)
            setPadding(dp(14), 0, dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply { topMargin = dp(8) }
        }
    }

    private fun crearUsuario(
        nombre: String,
        apellido: String,
        correo: String,
        contrasena: String,
        telefono: String?,
        rol: String
    ) {
        val sesion = token() ?: return
        RetrofitClient.apiService.crearUsuarioAdministrador(
            sesion,
            CrearUsuarioAdminRequest(nombre, apellido, correo, contrasena, telefono, rol)
        ).enqueue(respuestaOperacion("Usuario creado correctamente"))
    }

    private fun actualizarUsuario(
        idUsuario: Int,
        nombre: String,
        apellido: String,
        correo: String,
        telefono: String?,
        rol: String
    ) {
        val sesion = token() ?: return
        RetrofitClient.apiService.actualizarUsuarioAdministrador(
            sesion,
            idUsuario,
            ActualizarUsuarioAdminRequest(nombre, apellido, correo, telefono, rol)
        ).enqueue(respuestaOperacion("Usuario actualizado correctamente", idUsuario, nombre))
    }

    private fun confirmarEliminacion(usuario: AdminUsuario) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar usuario")
            .setMessage("¿Deseas eliminar a ${usuario.nombre}? Solo será posible si no tiene información asociada.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ -> eliminarUsuario(usuario.id_usuario) }
            .show()
    }

    private fun eliminarUsuario(idUsuario: Int) {
        val sesion = token() ?: return
        RetrofitClient.apiService.eliminarUsuarioAdministrador(
            sesion,
            idUsuario
        ).enqueue(respuestaOperacion("Usuario eliminado correctamente"))
    }

    private fun respuestaOperacion(
        mensajeExito: String,
        idUsuario: Int? = null,
        nombre: String? = null
    ): Callback<RespuestaUsuarioAdmin> {
        return object : Callback<RespuestaUsuarioAdmin> {
            override fun onResponse(
                call: Call<RespuestaUsuarioAdmin>,
                response: Response<RespuestaUsuarioAdmin>
            ) {
                if (response.isSuccessful) {
                    if (
                        idUsuario == getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
                            .getInt("id_usuario", 0) &&
                        !nombre.isNullOrBlank()
                    ) {
                        getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
                            .edit().putString("nombre", nombre).apply()
                    }
                    Toast.makeText(
                        this@AdministradorActivity,
                        response.body()?.mensaje ?: mensajeExito,
                        Toast.LENGTH_LONG
                    ).show()
                    cargarResumen()
                } else {
                    Toast.makeText(
                        this@AdministradorActivity,
                        mensajeError(response, "No fue posible completar la operación"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<RespuestaUsuarioAdmin>, t: Throwable) {
                Toast.makeText(
                    this@AdministradorActivity,
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
            val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pareja.forEachIndexed { indice, metrica ->
                fila.addView(TextView(this).apply {
                    text = "${metrica.second}\n${metrica.first}"
                    gravity = Gravity.CENTER
                    textSize = 15f
                    setTextColor(Color.parseColor("#211A24"))
                    setTypeface(null, Typeface.BOLD)
                    setBackgroundResource(R.drawable.bg_nav_item_active)
                    setPadding(dp(10), dp(16), dp(10), dp(16))
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (indice > 0) marginStart = dp(10)
                })
            }
            contenedor.addView(fila, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) })
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
                bloque.addView(fila(valor), LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) })
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

    private fun mensajeError(response: Response<*>, predeterminado: String): String {
        return try {
            val contenido = response.errorBody()?.string()
            if (contenido.isNullOrBlank()) {
                predeterminado
            } else {
                JSONObject(contenido).optString("mensaje", predeterminado)
            }
        } catch (error: Exception) {
            predeterminado
        }
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}

package com.example.evohkaapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : EvohkaActivity() {

    private var llamadaValidacionSesion: Call<RespuestaSesion>? = null
    private var dialogoValidacion: AlertDialog? = null
    private var restauracionEnCurso = false
    private var navegacionRealizada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val correo = findViewById<EditText>(R.id.correo)
        val password = findViewById<EditText>(R.id.password)

        val btnVerPassword =
            findViewById<ImageButton>(R.id.btnVerPassword)

        val grupoTipoIngreso =
            findViewById<RadioGroup>(R.id.grupo_tipo_ingreso)

        val btnContinuar =
            findViewById<Button>(R.id.btnContinuar)

        val txtRegistrarse =
            findViewById<TextView>(R.id.txtRegistrarse)

        val txtRecuperar =
            findViewById<TextView>(R.id.txtRecuperar)

        var passwordVisible = false

        btnVerPassword.setOnClickListener {

            passwordVisible = !passwordVisible

            if (passwordVisible) {

                password.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()

                btnVerPassword.setImageResource(
                    R.drawable.ic_visibility_off
                )

                btnVerPassword.contentDescription =
                    "Ocultar contraseña"

            } else {

                password.transformationMethod =
                    PasswordTransformationMethod.getInstance()

                btnVerPassword.setImageResource(
                    R.drawable.ic_visibility
                )

                btnVerPassword.contentDescription =
                    "Mostrar contraseña"
            }

            password.setSelection(password.text.length)
        }

        txtRegistrarse.setOnClickListener {

            val intent = Intent(
                this,
                RegistroActivity::class.java
            )

            startActivity(intent)
        }

        txtRecuperar.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RecuperarContrasenaActivity::class.java
                )
            )
        }

        btnContinuar.setOnClickListener {

            val correoIngresado =
                correo.text.toString().trim()

            val contrasenaIngresada =
                password.text.toString()

            if (
                correoIngresado.isEmpty() ||
                contrasenaIngresada.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Ingrese correo y contraseña",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val opcionSeleccionada =
                grupoTipoIngreso.checkedRadioButtonId

            if (opcionSeleccionada == -1) {

                Toast.makeText(
                    this,
                    "Seleccione el tipo de ingreso",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val radioSeleccionado =
                findViewById<RadioButton>(
                    opcionSeleccionada
                )

            val tipoIngresoSeleccionado =
                radioSeleccionado.text
                    .toString()
                    .lowercase()

            val datosLogin =
                LoginRequest(
                    correoIngresado,
                    contrasenaIngresada
                )

            RetrofitClient.apiService
                .login(datosLogin)
                .enqueue(
                    object : Callback<LoginResponse> {

                        override fun onResponse(
                            call: Call<LoginResponse>,
                            response: Response<LoginResponse>
                        ) {

                            if (
                                response.isSuccessful &&
                                response.body()?.ok == true
                            ) {

                                val respuesta =
                                    response.body()

                                val usuario =
                                    respuesta?.usuario

                                if (usuario == null) {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "No se pudo identificar el usuario",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return
                                }

                                val rolReal =
                                    usuario.rol
                                        .trim()
                                        .lowercase()

                                val rolSeleccionado =
                                    when (tipoIngresoSeleccionado) {
                                        "inquilino" -> "arrendatario"
                                        "propietario" -> "propietario"
                                        "administrador" -> "administrador"
                                        else -> ""
                                    }

                                if (rolReal != rolSeleccionado) {

                                    val mensajeRol =
                                        if (rolReal == "arrendatario") {
                                            "Este usuario está registrado como inquilino"
                                        } else if (
                                            rolReal == "propietario"
                                        ) {
                                            "Este usuario está registrado como propietario"
                                        } else if (
                                            rolReal == "administrador"
                                        ) {
                                            "Este usuario está registrado como administrador"
                                        } else {
                                            "El rol de este usuario no está habilitado"
                                        }

                                    Toast.makeText(
                                        this@MainActivity,
                                        mensajeRol,
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return
                                }

                                val token =
                                    respuesta.token

                                if (token.isNullOrBlank()) {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "No se pudo iniciar una sesión válida",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return
                                }

                                try {
                                    SesionSegura.guardarSesion(
                                        token,
                                        usuario.copy(rol = rolReal),
                                        tipoIngresoSeleccionado
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "No se pudo proteger la sesión en este dispositivo",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return
                                }

                                NavegacionSesionInvalida.reiniciar()

                                Toast.makeText(
                                    this@MainActivity,
                                    "Bienvenido ${usuario.nombre}",
                                    Toast.LENGTH_LONG
                                ).show()

                                abrirPantallaPrincipal(
                                    rolReal
                                )

                            } else {

                                Toast.makeText(
                                    this@MainActivity,
                                    "Correo o contraseña incorrectos",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<LoginResponse>,
                            t: Throwable
                        ) {

                            Toast.makeText(
                                this@MainActivity,
                                "No se pudo conectar con el servidor",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
        }

        intent.getStringExtra(
            NavegacionSesionInvalida.EXTRA_MENSAJE_SESION
        )?.let { mensaje ->
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            intent.removeExtra(
                NavegacionSesionInvalida.EXTRA_MENSAJE_SESION
            )
        }

        restaurarSesionSiExiste()
    }

    private fun restaurarSesionSiExiste() {
        if (restauracionEnCurso || navegacionRealizada) {
            return
        }

        val token = SesionSegura.obtenerTokenParaValidacion()
        if (token.isNullOrBlank()) {
            return
        }

        restauracionEnCurso = true
        mostrarProgresoValidacion()

        val llamada = RetrofitClient.apiService.validarSesion(token)
        llamadaValidacionSesion = llamada
        llamada.enqueue(
            object : Callback<RespuestaSesion> {
                override fun onResponse(
                    call: Call<RespuestaSesion>,
                    response: Response<RespuestaSesion>
                ) {
                    if (call.isCanceled || isFinishing) {
                        return
                    }

                    finalizarProgresoValidacion()

                    val usuario = response.body()?.usuario
                    if (
                        response.isSuccessful &&
                        response.body()?.ok == true &&
                        usuario != null
                    ) {
                        val rolActual = usuario.rol.trim().lowercase()
                        try {
                            SesionSegura.actualizarIdentidad(
                                usuario.copy(rol = rolActual)
                            )
                        } catch (_: Exception) {
                            SesionSegura.cerrarSesion()
                            Toast.makeText(
                                this@MainActivity,
                                "No se pudo actualizar la sesión local. Inicia sesión nuevamente.",
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }
                        SesionSegura.marcarValidadaEnEsteProceso()
                        NavegacionSesionInvalida.reiniciar()

                        if (!abrirPantallaPrincipal(rolActual)) {
                            SesionSegura.cerrarSesion()
                            Toast.makeText(
                                this@MainActivity,
                                "El rol actual no está habilitado en EVOHKA",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return
                    }

                    if (
                        PoliticaRestauracionSesion.paraCodigoHttp(
                            response.code()
                        ) == AccionRestauracionSesion.LIMPIAR_Y_SOLICITAR_LOGIN
                    ) {
                        SesionSegura.cerrarSesion()
                        Toast.makeText(
                            this@MainActivity,
                            "La sesión venció o dejó de ser válida. Inicia sesión nuevamente.",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val mensaje = if (response.code() == 403) {
                        "El servidor no permitió validar esta sesión. La sesión local se conservará."
                    } else if (response.code() >= 500) {
                        "El servidor no está disponible temporalmente. La sesión local se conservará."
                    } else {
                        "No fue posible validar la sesión. La sesión local se conservará."
                    }
                    mostrarReintentoValidacion(mensaje)
                }

                override fun onFailure(
                    call: Call<RespuestaSesion>,
                    t: Throwable
                ) {
                    if (call.isCanceled || isFinishing) {
                        return
                    }

                    finalizarProgresoValidacion()
                    if (
                        PoliticaRestauracionSesion.paraErrorDeRed() ==
                        AccionRestauracionSesion.CONSERVAR_Y_REINTENTAR
                    ) {
                        mostrarReintentoValidacion(
                            "No se pudo conectar con el servidor. La sesión local se conservará."
                        )
                    }
                }
            }
        )
    }

    private fun mostrarProgresoValidacion() {
        dialogoValidacion?.dismiss()
        dialogoValidacion = AlertDialog.Builder(this)
            .setMessage("Validando sesión...")
            .setCancelable(false)
            .create()
            .also(AlertDialog::show)
    }

    private fun finalizarProgresoValidacion() {
        restauracionEnCurso = false
        llamadaValidacionSesion = null
        dialogoValidacion?.dismiss()
        dialogoValidacion = null
    }

    private fun mostrarReintentoValidacion(mensaje: String) {
        if (isFinishing || isDestroyed) {
            return
        }

        dialogoValidacion?.dismiss()
        dialogoValidacion = AlertDialog.Builder(this)
            .setTitle("No se pudo validar la sesión")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("Reintentar") { _, _ ->
                dialogoValidacion = null
                restaurarSesionSiExiste()
            }
            .setNegativeButton("Cerrar sesión") { _, _ ->
                dialogoValidacion = null
                SesionSegura.cerrarSesion()
                NavegacionSesionInvalida.reiniciar()
            }
            .create()
            .also(AlertDialog::show)
    }

    private fun abrirPantallaPrincipal(
        rol: String
    ): Boolean {

        if (navegacionRealizada) {
            return true
        }

        val intent =
            when (
                rol.trim().lowercase()
            ) {

                "propietario" ->
                    Intent(
                        this,
                        InicioPropietarioActivity::class.java
                    )

                "arrendatario" ->
                    Intent(
                        this,
                        PropiedadesDisponiblesActivity::class.java
                    )

                "administrador" ->
                    Intent(
                        this,
                        AdministradorActivity::class.java
                    )

                else ->
                    return false
            }

        navegacionRealizada = true

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()

        return true
    }

    override fun onDestroy() {
        llamadaValidacionSesion?.cancel()
        llamadaValidacionSesion = null
        dialogoValidacion?.dismiss()
        dialogoValidacion = null
        super.onDestroy()
    }
}

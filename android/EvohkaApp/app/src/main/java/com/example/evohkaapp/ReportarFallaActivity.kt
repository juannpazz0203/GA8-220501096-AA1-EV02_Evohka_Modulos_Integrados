package com.example.evohkaapp

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportarFallaActivity : EvohkaActivity() {

    private lateinit var spinnerPropiedad: Spinner
    private lateinit var spinnerCategoria: Spinner
    private lateinit var descripcion: EditText
    private lateinit var btnEnviar: Button
    private lateinit var txtEstado: TextView
    private var propiedades = emptyList<PropiedadFalla>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportar_falla)

        spinnerPropiedad = findViewById(R.id.spinnerPropiedadFalla)
        spinnerCategoria = findViewById(R.id.spinnerCategoriaFalla)
        descripcion = findViewById(R.id.editDescripcionFalla)
        btnEnviar = findViewById(R.id.btnEnviarFalla)
        txtEstado = findViewById(R.id.txtEstadoReporteFalla)

        spinnerCategoria.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Plomería", "Electricidad", "Estructura", "Gas", "Otro")
        )
        btnEnviar.isEnabled = false
        btnEnviar.setOnClickListener { enviarFalla() }
        cargarPropiedades()
    }

    private fun token() = getSharedPreferences("EvohkaSesion", MODE_PRIVATE)
        .getString("token", null)

    private fun cargarPropiedades() {
        val sesion = token()
        if (sesion.isNullOrBlank()) {
            txtEstado.text = "Sesión no válida"
            return
        }
        txtEstado.text = "Consultando inmuebles habilitados..."
        RetrofitClient.apiService.listarPropiedadesParaFallas(sesion).enqueue(
            object : Callback<RespuestaPropiedadesFalla> {
                override fun onResponse(
                    call: Call<RespuestaPropiedadesFalla>,
                    response: Response<RespuestaPropiedadesFalla>
                ) {
                    propiedades = response.body()?.datos ?: emptyList()
                    spinnerPropiedad.adapter = ArrayAdapter(
                        this@ReportarFallaActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        propiedades.map { "${it.titulo} · ${it.direccion.orEmpty()}" }
                    )
                    btnEnviar.isEnabled = response.isSuccessful && propiedades.isNotEmpty()
                    txtEstado.text = if (propiedades.isEmpty()) {
                        "No tienes un contrato activo para reportar fallas"
                    } else {
                        "Selecciona el inmueble y describe la falla"
                    }
                }

                override fun onFailure(call: Call<RespuestaPropiedadesFalla>, t: Throwable) {
                    txtEstado.text = "No se pudo conectar con el servidor"
                }
            }
        )
    }

    private fun enviarFalla() {
        val propiedad = propiedades.getOrNull(spinnerPropiedad.selectedItemPosition) ?: return
        val detalle = descripcion.text.toString().trim()
        if (detalle.isBlank()) {
            descripcion.error = "Describe la falla"
            return
        }
        val sesion = token() ?: return
        btnEnviar.isEnabled = false
        RetrofitClient.apiService.crearFalla(
            sesion,
            CrearFallaRequest(
                propiedad.id_propiedad,
                spinnerCategoria.selectedItem.toString(),
                detalle
            )
        ).enqueue(object : Callback<RespuestaFalla> {
            override fun onResponse(
                call: Call<RespuestaFalla>,
                response: Response<RespuestaFalla>
            ) {
                btnEnviar.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@ReportarFallaActivity, "Falla reportada", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    txtEstado.text = "No fue posible reportar la falla"
                }
            }

            override fun onFailure(call: Call<RespuestaFalla>, t: Throwable) {
                btnEnviar.isEnabled = true
                txtEstado.text = "No se pudo conectar con el servidor"
            }
        })
    }
}

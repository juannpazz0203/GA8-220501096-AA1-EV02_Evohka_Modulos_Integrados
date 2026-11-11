package com.example.evohkaapp

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

class CrearContratoActivity : EvohkaActivity() {

    private var fechaInicioIso = ""
    private var fechaFinIso = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_contrato)

        val idSolicitud = intent.getIntExtra(
            "id_solicitud",
            0
        )

        if (idSolicitud <= 0) {
            Toast.makeText(
                this,
                "No fue posible identificar la solicitud",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val tituloPropiedad = intent.getStringExtra(
            "titulo_propiedad"
        ) ?: "Inmueble"

        val canon = intent.getDoubleExtra(
            "canon_propiedad",
            0.0
        )

        val txtSolicitud =
            findViewById<TextView>(R.id.txtContratoSolicitud)
        val etFechaInicio =
            findViewById<EditText>(R.id.etFechaInicioContrato)
        val etFechaFin =
            findViewById<EditText>(R.id.etFechaFinContrato)
        val etValorMensual =
            findViewById<EditText>(R.id.etValorMensualContrato)
        val etDeposito =
            findViewById<EditText>(R.id.etDepositoContrato)
        val etDescripcion =
            findViewById<EditText>(R.id.etDescripcionContrato)
        val btnCrear =
            findViewById<Button>(R.id.btnCrearContrato)
        val btnCancelar =
            findViewById<Button>(R.id.btnCancelarContrato)

        txtSolicitud.text =
            "Solicitud aprobada · $tituloPropiedad"

        if (canon > 0) {
            etValorMensual.setText(
                String.format(
                    Locale.US,
                    "%.2f",
                    canon
                )
            )
        }

        etFechaInicio.setOnClickListener {
            seleccionarFecha(etFechaInicio) {
                fechaInicioIso = it
            }
        }

        etFechaFin.setOnClickListener {
            seleccionarFecha(etFechaFin) {
                fechaFinIso = it
            }
        }

        btnCancelar.setOnClickListener {
            finish()
        }

        btnCrear.setOnClickListener {
            crearContrato(
                idSolicitud,
                etValorMensual,
                etDeposito,
                etDescripcion,
                btnCrear
            )
        }
    }

    private fun seleccionarFecha(
        campo: EditText,
        alSeleccionar: (String) -> Unit
    ) {
        val calendario = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                val seleccion = Calendar.getInstance().apply {
                    set(anio, mes, dia, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                campo.setText(
                    FormatoEvohka.fechaVisible(
                        seleccion.time
                    )
                )

                alSeleccionar(
                    FormatoEvohka.fechaIso(
                        seleccion.time
                    )
                )
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun crearContrato(
        idSolicitud: Int,
        etValorMensual: EditText,
        etDeposito: EditText,
        etDescripcion: EditText,
        btnCrear: Button
    ) {
        if (
            fechaInicioIso.isBlank() ||
            fechaFinIso.isBlank()
        ) {
            Toast.makeText(
                this,
                "Selecciona las fechas del contrato",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (fechaFinIso <= fechaInicioIso) {
            Toast.makeText(
                this,
                "La fecha final debe ser posterior a la inicial",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val valorMensual = etValorMensual.text
            .toString()
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()

        if (valorMensual == null || valorMensual <= 0) {
            Toast.makeText(
                this,
                "Ingresa un valor mensual válido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val textoDeposito = etDeposito.text
            .toString()
            .trim()

        val deposito =
            if (textoDeposito.isBlank()) {
                null
            } else {
                textoDeposito
                    .replace(',', '.')
                    .toDoubleOrNull()
            }

        if (
            textoDeposito.isNotBlank() &&
            (deposito == null || deposito < 0)
        ) {
            Toast.makeText(
                this,
                "Ingresa un depósito válido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val token = SesionSegura.obtenerToken()

        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "La sesión no es válida",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnCrear.isEnabled = false
        btnCrear.text = "Creando contrato..."

        val descripcion = etDescripcion.text
            .toString()
            .trim()
            .ifBlank { null }

        RetrofitClient.apiService.crearContrato(
            token,
            CrearContratoRequest(
                id_solicitud = idSolicitud,
                fecha_inicio = fechaInicioIso,
                fecha_fin = fechaFinIso,
                valor_mensual = valorMensual,
                deposito = deposito,
                descripcion = descripcion
            )
        ).enqueue(
            object : Callback<RespuestaContrato> {
                override fun onResponse(
                    call: Call<RespuestaContrato>,
                    response: Response<RespuestaContrato>
                ) {
                    btnCrear.isEnabled = true
                    btnCrear.text = "Crear contrato"

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@CrearContratoActivity,
                            response.body()?.mensaje
                                ?: "Contrato creado correctamente",
                            Toast.LENGTH_LONG
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@CrearContratoActivity,
                            mensajeError(
                                response,
                                "No se pudo crear el contrato"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaContrato>,
                    t: Throwable
                ) {
                    btnCrear.isEnabled = true
                    btnCrear.text = "Crear contrato"

                    Toast.makeText(
                        this@CrearContratoActivity,
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun mensajeError(
        response: Response<*>,
        predeterminado: String
    ): String {
        return try {
            val contenido = response.errorBody()?.string()

            if (contenido.isNullOrBlank()) {
                predeterminado
            } else {
                JSONObject(contenido)
                    .optString("mensaje", predeterminado)
            }
        } catch (error: Exception) {
            predeterminado
        }
    }
}

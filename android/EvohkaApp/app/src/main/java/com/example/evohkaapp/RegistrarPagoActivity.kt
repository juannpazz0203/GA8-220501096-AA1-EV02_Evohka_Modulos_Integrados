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

class RegistrarPagoActivity : EvohkaActivity() {

    private var fechaPagoIso = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_pago)

        val idContrato = intent.getIntExtra(
            "id_contrato",
            0
        )

        if (idContrato <= 0) {
            Toast.makeText(
                this,
                "No fue posible identificar el contrato",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val etFecha = findViewById<EditText>(R.id.etFechaPago)
        val etMonto = findViewById<EditText>(R.id.etMontoPago)
        val etMetodo = findViewById<EditText>(R.id.etMetodoPago)
        val etObservaciones =
            findViewById<EditText>(R.id.etObservacionesPago)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPago)

        val tituloPropiedad = intent.getStringExtra(
            "titulo_propiedad"
        ) ?: "Inmueble"

        findViewById<TextView>(R.id.txtPagoContrato).text =
            "Contrato #$idContrato · $tituloPropiedad"

        val valorMensual = intent.getDoubleExtra(
            "valor_mensual",
            0.0
        )

        if (valorMensual > 0) {
            etMonto.setText(
                String.format(
                    Locale.US,
                    "%.2f",
                    valorMensual
                )
            )
        }

        val hoy = Calendar.getInstance()
        fechaPagoIso = FormatoEvohka.fechaIso(hoy.time)
        etFecha.setText(FormatoEvohka.fechaVisible(hoy.time))

        etFecha.setOnClickListener {
            seleccionarFecha(etFecha)
        }

        findViewById<Button>(R.id.btnCancelarPago)
            .setOnClickListener {
                finish()
            }

        btnGuardar.setOnClickListener {
            guardarPago(
                idContrato,
                etMonto,
                etMetodo,
                etObservaciones,
                btnGuardar
            )
        }
    }

    private fun seleccionarFecha(campo: EditText) {
        val calendario = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                val seleccion = Calendar.getInstance().apply {
                    set(anio, mes, dia, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                fechaPagoIso =
                    FormatoEvohka.fechaIso(seleccion.time)
                campo.setText(
                    FormatoEvohka.fechaVisible(seleccion.time)
                )
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun guardarPago(
        idContrato: Int,
        etMonto: EditText,
        etMetodo: EditText,
        etObservaciones: EditText,
        btnGuardar: Button
    ) {
        val monto = etMonto.text
            .toString()
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()

        if (monto == null || monto <= 0) {
            Toast.makeText(
                this,
                "Ingresa un monto válido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val metodo = etMetodo.text
            .toString()
            .trim()

        if (metodo.isBlank()) {
            Toast.makeText(
                this,
                "Ingresa el método de pago",
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

        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando pago..."

        RetrofitClient.apiService.registrarPago(
            token,
            CrearPagoRequest(
                id_contrato = idContrato,
                fecha_pago = fechaPagoIso,
                monto = monto,
                metodo_pago = metodo,
                observaciones = etObservaciones.text
                    .toString()
                    .trim()
                    .ifBlank { null }
            )
        ).enqueue(
            object : Callback<RespuestaPago> {
                override fun onResponse(
                    call: Call<RespuestaPago>,
                    response: Response<RespuestaPago>
                ) {
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar pago"

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@RegistrarPagoActivity,
                            response.body()?.mensaje
                                ?: "Pago registrado correctamente",
                            Toast.LENGTH_LONG
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@RegistrarPagoActivity,
                            mensajeError(
                                response,
                                "No se pudo registrar el pago"
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<RespuestaPago>,
                    t: Throwable
                ) {
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar pago"

                    Toast.makeText(
                        this@RegistrarPagoActivity,
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

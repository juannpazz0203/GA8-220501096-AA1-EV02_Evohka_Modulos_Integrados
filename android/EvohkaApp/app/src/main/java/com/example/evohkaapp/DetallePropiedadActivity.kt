package com.example.evohkaapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale

class DetallePropiedadActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_detalle_propiedad)

        val txtTitulo =
            findViewById<TextView>(R.id.txt_titulo_detalle)

        val txtCiudad =
            findViewById<TextView>(R.id.txt_ciudad_detalle)

        val txtDireccion =
            findViewById<TextView>(R.id.txt_direccion_detalle)

        val txtPrecio =
            findViewById<TextView>(R.id.txt_precio_detalle)

        val txtEstado =
            findViewById<TextView>(R.id.txt_estado_detalle)

        val btnSolicitarArriendo =
            findViewById<Button>(R.id.btn_solicitar_arriendo)

        val btnVolver =
            findViewById<Button>(R.id.btn_volver_detalle)

        val titulo =
            intent.getStringExtra("titulo")
                ?: "Inmueble"

        val ciudad =
            intent.getStringExtra("ciudad")
                ?: "Sin ciudad"

        val direccion =
            intent.getStringExtra("direccion")
                ?: "Sin dirección"

        val precio =
            intent.getDoubleExtra(
                "precio_mensual",
                0.0
            )

        val estado =
            intent.getStringExtra("estado")
                ?: "Disponible"

        val formatoMoneda =
            NumberFormat.getCurrencyInstance(
                Locale("es", "CO")
            )

        txtTitulo.text =
            titulo

        txtCiudad.text =
            "Ciudad: $ciudad"

        txtDireccion.text =
            "Dirección: $direccion"

        txtPrecio.text =
            "Canon mensual: ${formatoMoneda.format(precio)}"

        txtEstado.text =
            "Estado: $estado"

        btnVolver.setOnClickListener {
            finish()
        }

        btnSolicitarArriendo.setOnClickListener {

            Toast.makeText(
                this,
                "Módulo de solicitud de arriendo no incluido en esta evidencia",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
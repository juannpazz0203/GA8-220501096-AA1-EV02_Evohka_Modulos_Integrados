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

        val txtTipo =
            findViewById<TextView>(R.id.txt_tipo_detalle)

        val txtCiudad =
            findViewById<TextView>(R.id.txt_ciudad_detalle)

        val txtBarrio =
            findViewById<TextView>(R.id.txt_barrio_detalle)

        val txtDireccion =
            findViewById<TextView>(R.id.txt_direccion_detalle)

        val txtCanon =
            findViewById<TextView>(R.id.txt_canon_detalle)

        val txtHabitaciones =
            findViewById<TextView>(R.id.txt_habitaciones_detalle)

        val txtBanos =
            findViewById<TextView>(R.id.txt_banos_detalle)

        val btnSolicitarArriendo =
            findViewById<Button>(R.id.btn_solicitar_arriendo)

        val btnVolver =
            findViewById<Button>(R.id.btn_volver_detalle)

        val idPropiedad =
            intent.getIntExtra(
                "id_propiedad",
                0
            )

        val titulo =
            intent.getStringExtra("titulo")
                ?: "Inmueble"

        val tipoInmueble =
            intent.getStringExtra("tipo_inmueble")
                ?: "No definido"

        val direccion =
            intent.getStringExtra("direccion")
                ?: "Sin dirección"

        val barrio =
            intent.getStringExtra("barrio")
                ?: "Sin barrio"

        val ciudad =
            intent.getStringExtra("ciudad")
                ?: "Sin ciudad"

        val canon =
            intent.getDoubleExtra(
                "canon",
                0.0
            )

        val habitaciones =
            intent.getIntExtra(
                "habitaciones",
                0
            )

        val banos =
            intent.getIntExtra(
                "banos",
                0
            )

        val formatoMoneda =
            NumberFormat.getCurrencyInstance(
                Locale("es", "CO")
            )

        txtTitulo.text =
            titulo

        txtTipo.text =
            "Tipo: $tipoInmueble"

        txtCiudad.text =
            "Ciudad: $ciudad"

        txtBarrio.text =
            "Barrio: $barrio"

        txtDireccion.text =
            "Dirección: $direccion"

        txtCanon.text =
            "Canon mensual: ${formatoMoneda.format(canon)}"

        txtHabitaciones.text =
            "Habitaciones: $habitaciones"

        txtBanos.text =
            "Baños: $banos"

        btnVolver.setOnClickListener {
            finish()
        }

        btnSolicitarArriendo.setOnClickListener {

            Toast.makeText(
                this,
                "Solicitud de arriendo para propiedad #$idPropiedad pendiente de implementar",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
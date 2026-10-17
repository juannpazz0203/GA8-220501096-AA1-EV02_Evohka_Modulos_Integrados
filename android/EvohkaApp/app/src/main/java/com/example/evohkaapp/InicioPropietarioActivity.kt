package com.example.evohkaapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class InicioPropietarioActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_inicio_propietario)

        val txtSaludoPropietario =
            findViewById<TextView>(R.id.txtSaludoPropietario)

        val btnMisInmuebles =
            findViewById<Button>(R.id.btnMisInmuebles)

        val btnSolicitudes =
            findViewById<Button>(R.id.btnSolicitudes)

        val btnVisitas =
            findViewById<Button>(R.id.btnVisitas)

        val btnPublicarInmueble =
            findViewById<Button>(R.id.btnPublicarInmueble)

        val btnCerrarSesion =
            findViewById<Button>(R.id.btnCerrarSesion)

        val preferencias =
            getSharedPreferences(
                "EvohkaSesion",
                MODE_PRIVATE
            )

        val nombreUsuario =
            preferencias.getString(
                "nombre",
                "usuario"
            )

        txtSaludoPropietario.text =
            "Hola, $nombreUsuario 👋"

        btnMisInmuebles.setOnClickListener {

            val intent = Intent(
                this,
                MisInmueblesActivity::class.java
            )

            startActivity(intent)
        }

        btnPublicarInmueble.setOnClickListener {

            val intent = Intent(
                this,
                PublicarInmuebleActivity::class.java
            )

            startActivity(intent)
        }

        btnSolicitudes.setOnClickListener {

            Toast.makeText(
                this,
                "Módulo no incluido en esta evidencia",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnVisitas.setOnClickListener {

            Toast.makeText(
                this,
                "Módulo no incluido en esta evidencia",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {

        val preferencias =
            getSharedPreferences(
                "EvohkaSesion",
                MODE_PRIVATE
            )

        preferencias.edit()
            .clear()
            .apply()

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }
}
package com.example.evohkaapp

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class RegistroActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val btnVolver = findViewById<TextView>(R.id.btnVolver)

        btnVolver.setOnClickListener {
            finish()
        }
    }
}
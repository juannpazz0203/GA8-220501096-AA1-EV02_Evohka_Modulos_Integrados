package com.example.evohkaapp

import android.app.Application

class EvohkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SesionSegura.inicializar(this)
        NavegacionSesionInvalida.inicializar(this)
    }
}

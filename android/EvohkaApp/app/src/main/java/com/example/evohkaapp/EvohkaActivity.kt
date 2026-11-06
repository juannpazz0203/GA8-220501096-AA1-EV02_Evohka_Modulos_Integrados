package com.example.evohkaapp

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

open class EvohkaActivity : Activity() {

    private var paddingBaseSistema: IntArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    override fun setContentView(layoutResID: Int) {
        paddingBaseSistema = null
        super.setContentView(layoutResID)
        aplicarInsetsSistema()
    }

    override fun setContentView(view: View?) {
        paddingBaseSistema = null
        super.setContentView(view)
        aplicarInsetsSistema()
    }

    override fun onResume() {
        super.onResume()
        NavegacionEvohka.actualizarIndicadorNotificaciones(this)
    }

    protected fun aplicarInsetsSistema(
        incluirTeclado: Boolean = false
    ) {
        val contenido = findViewById<View>(android.R.id.content)
            ?: return

        val paddingBase = paddingBaseSistema ?: intArrayOf(
            contenido.paddingLeft,
            contenido.paddingTop,
            contenido.paddingRight,
            contenido.paddingBottom
        ).also { paddingBaseSistema = it }

        ViewCompat.setOnApplyWindowInsetsListener(
            contenido
        ) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            val teclado = if (incluirTeclado) {
                insets.getInsets(
                    WindowInsetsCompat.Type.ime()
                ).bottom
            } else {
                0
            }

            vista.setPadding(
                paddingBase[0] + barras.left,
                paddingBase[1] + barras.top,
                paddingBase[2] + barras.right,
                paddingBase[3] + maxOf(barras.bottom, teclado)
            )

            insets
        }

        ViewCompat.requestApplyInsets(contenido)
    }
}

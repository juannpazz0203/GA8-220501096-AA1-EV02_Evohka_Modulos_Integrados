package com.example.evohkaapp

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicBoolean

internal object PoliticaRespuestaSesion {
    fun debeInvalidar(
        codigoHttp: Int,
        tieneAutorizacion: Boolean,
        esValidacionInicial: Boolean
    ): Boolean =
        codigoHttp == 401 && tieneAutorizacion && !esValidacionInicial
}

internal enum class AccionRestauracionSesion {
    LIMPIAR_Y_SOLICITAR_LOGIN,
    CONSERVAR_Y_REINTENTAR
}

internal object PoliticaRestauracionSesion {
    fun paraCodigoHttp(codigoHttp: Int): AccionRestauracionSesion =
        if (codigoHttp == 401) {
            AccionRestauracionSesion.LIMPIAR_Y_SOLICITAR_LOGIN
        } else {
            AccionRestauracionSesion.CONSERVAR_Y_REINTENTAR
        }

    fun paraErrorDeRed(): AccionRestauracionSesion =
        AccionRestauracionSesion.CONSERVAR_Y_REINTENTAR
}

class SesionHttpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val solicitud = chain.request()
        val respuesta = chain.proceed(solicitud)
        val tieneAutorizacion = !solicitud.header("Authorization").isNullOrBlank()
        val esValidacionInicial =
            solicitud.url.encodedPath == "/api/auth/sesion"

        if (
            PoliticaRespuestaSesion.debeInvalidar(
                respuesta.code,
                tieneAutorizacion,
                esValidacionInicial
            )
        ) {
            NavegacionSesionInvalida.notificar()
        }

        return respuesta
    }
}

object NavegacionSesionInvalida {
    const val EXTRA_MENSAJE_SESION = "mensaje_sesion_invalida"

    private val redireccionEnCurso = AtomicBoolean(false)

    @Volatile
    private var contextoAplicacion: Context? = null

    fun inicializar(contexto: Context) {
        contextoAplicacion = contexto.applicationContext
    }

    fun notificar() {
        if (!redireccionEnCurso.compareAndSet(false, true)) {
            return
        }

        SesionSegura.cerrarSesion()
        val contexto = contextoAplicacion ?: return
        Handler(Looper.getMainLooper()).post {
            runCatching {
                contexto.startActivity(
                    Intent(contexto, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(
                            EXTRA_MENSAJE_SESION,
                            "La sesión venció o dejó de ser válida. Inicia sesión nuevamente."
                        )
                    }
                )
            }
        }
    }

    fun reiniciar() {
        redireccionEnCurso.set(false)
    }
}

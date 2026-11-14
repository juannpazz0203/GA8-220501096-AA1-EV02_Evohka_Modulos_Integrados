package com.example.evohkaapp

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import coil3.ImageLoader
import coil3.load
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

internal data class EstadoDescargaFotografia(
    val codigoHttp: Int? = null,
    val tipoContenido: String? = null,
    val tamanoBytes: Long? = null,
    val errorRed: Boolean = false
)

internal fun describirFalloFotografia(
    estado: EstadoDescargaFotografia?,
    error: Throwable? = null,
    etapaSinRespuestaHttp: String = "Error antes de la descarga HTTP"
): String {
    val resumen = when {
        estado?.errorRed == true -> "Error de red"
        estado?.codigoHttp != null && estado.codigoHttp !in 200..299 ->
            "HTTP ${estado.codigoHttp}"
        estado?.codigoHttp != null &&
            !estado.tipoContenido.orEmpty().startsWith("image/") ->
            "La respuesta no es una imagen " +
                "(HTTP ${estado.codigoHttp}, ${estado.tipoContenido ?: "tipo desconocido"})"
        estado?.codigoHttp != null -> {
            val tamano = estado.tamanoBytes
                ?.takeIf { it >= 0 }
                ?.let { ", $it bytes" }
                .orEmpty()
            "Error de decodificación " +
                "(HTTP ${estado.codigoHttp}, ${estado.tipoContenido}$tamano)"
        }
        else -> etapaSinRespuestaHttp
    }

    if (error == null) return resumen

    val causaRaiz = causaRaizFotografia(error)
    val mensaje = mensajeSeguroFotografia(
        error.message ?: causaRaiz.message
    )
    return buildString {
        append(resumen)
        append(" · excepción=")
        append(error.javaClass.simpleName.ifBlank { error.javaClass.name })
        append(" · causa=")
        append(
            causaRaiz.javaClass.simpleName.ifBlank {
                causaRaiz.javaClass.name
            }
        )
        if (!mensaje.isNullOrBlank()) {
            append(" · detalle=")
            append(mensaje)
        }
    }
}

internal fun claveDiagnosticoFotografia(url: String): String =
    runCatching { URI(url).rawPath }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: url

private fun causaRaizFotografia(error: Throwable): Throwable {
    var actual = error
    val visitadas = mutableSetOf<Throwable>()
    while (actual.cause != null && visitadas.add(actual)) {
        val siguiente = checkNotNull(actual.cause)
        if (siguiente === actual) break
        actual = siguiente
    }
    return actual
}

internal fun mensajeSeguroFotografia(mensaje: String?): String? {
    if (mensaje.isNullOrBlank()) return null

    return mensaje
        .replace(Regex("(?i)Bearer\\s+\\S+"), "[credencial]")
        .replace(
            Regex("[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"),
            "[token]"
        )
        .replace(Regex("(?i)https?://\\S+"), "[url]")
        .replace(Regex("[A-Za-z]:[\\\\/][^\\r\\n]*"), "[ruta]")
        .replace(
            Regex(
                "(?i)(?<![A-Za-z0-9])/(?:data|storage|sdcard|system|vendor|apex|mnt|cache|tmp)/[^\\r\\n]*"
            ),
            "[ruta]"
        )
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(MAXIMO_MENSAJE_DIAGNOSTICO)
        .ifBlank { null }
}

private object RegistroDescargaFotografias {
    private val estados = ConcurrentHashMap<String, EstadoDescargaFotografia>()

    fun registrar(url: String, estado: EstadoDescargaFotografia) {
        estados[url] = estado
    }

    fun limpiar(url: String) {
        estados.remove(url)
    }

    fun consumir(url: String): EstadoDescargaFotografia? = estados.remove(url)
}

internal fun Request.conAutorizacionFotografia(token: String?): Request =
    if (token.isNullOrBlank()) {
        this
    } else {
        newBuilder()
            .header("Authorization", token)
            .build()
    }

private class AutorizacionImagenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val solicitud = chain.request()
        val token = SesionSegura.obtenerToken()
        val autorizada = solicitud.conAutorizacionFotografia(token)
        val ruta = autorizada.url.encodedPath
        val tieneAutorizacion =
            !autorizada.header("Authorization").isNullOrBlank()
        RegistroDescargaFotografias.limpiar(ruta)

        return try {
            val respuesta = chain.proceed(autorizada)
            val tipoContenido = respuesta.header("Content-Type")
                ?.substringBefore(';')
                ?: "ausente"
            val tamanoBytes = respuesta.body?.contentLength()
            RegistroDescargaFotografias.registrar(
                ruta,
                EstadoDescargaFotografia(
                    codigoHttp = respuesta.code,
                    tipoContenido = tipoContenido,
                    tamanoBytes = tamanoBytes
                )
            )
            val diagnostico =
                "ruta=$ruta estado=${respuesta.code} " +
                    "tipo=$tipoContenido tamano=$tamanoBytes " +
                    "autorizacion=$tieneAutorizacion"

            if (
                respuesta.isSuccessful &&
                tipoContenido.equals("image/webp", ignoreCase = true)
            ) {
                Log.d(ETIQUETA_FOTOS, diagnostico)
            } else {
                Log.w(ETIQUETA_FOTOS, diagnostico)
            }
            respuesta
        } catch (error: Exception) {
            RegistroDescargaFotografias.registrar(
                ruta,
                EstadoDescargaFotografia(errorRed = true)
            )
            Log.e(
                ETIQUETA_FOTOS,
                "ruta=$ruta error_red=${error.javaClass.simpleName} " +
                    "autorizacion=$tieneAutorizacion"
            )
            throw error
        }
    }
}

internal object CargadorImagenesEvohka {
    @Volatile
    private var instancia: ImageLoader? = null

    fun obtener(contexto: Context): ImageLoader =
        instancia ?: synchronized(this) {
            instancia ?: crear(contexto.applicationContext).also {
                instancia = it
            }
        }

    private fun crear(contexto: Context): ImageLoader {
        val cliente = OkHttpClient.Builder()
            .addInterceptor(AutorizacionImagenInterceptor())
            .addInterceptor(SesionHttpInterceptor())
            .build()

        return ImageLoader.Builder(contexto)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { cliente }
                    )
                )
            }
            .build()
    }
}

internal fun ImageView.cargarFotografia(rutaHttp: String?) {
    if (rutaHttp.isNullOrBlank()) {
        setImageResource(R.drawable.ic_foto_neutra_evohka)
        Log.w(ETIQUETA_FOTOS, "ruta_fotografia_ausente")
        return
    }

    setImageResource(R.drawable.ic_foto_neutra_evohka)
    val destino = runCatching {
        BackendUrlProvider.resolverRutaHttp(rutaHttp)
    }.onFailure { error ->
        Log.e(
            ETIQUETA_FOTOS,
            "ruta_relativa_invalida error=${error.javaClass.simpleName}"
        )
    }.getOrNull()
    if (destino == null) {
        return
    }
    val claveDiagnostico = claveDiagnosticoFotografia(destino)

    runCatching {
        load(destino, CargadorImagenesEvohka.obtener(context)) {
            crossfade(true)
            listener(
                onSuccess = { _, _ ->
                    RegistroDescargaFotografias.limpiar(claveDiagnostico)
                },
                onError = { _, resultado ->
                    val estado =
                        RegistroDescargaFotografias.consumir(claveDiagnostico)
                    val diagnostico = describirFalloFotografia(
                        estado,
                        resultado.throwable
                    )
                    Log.e(
                        ETIQUETA_FOTOS,
                        "coil_error ruta=$rutaHttp " +
                            "diagnostico=$diagnostico " +
                            "error=${resultado.throwable.javaClass.simpleName} " +
                            "causa=${resultado.throwable.cause?.javaClass?.simpleName ?: "ausente"}"
                    )
                    post {
                        setImageResource(R.drawable.ic_foto_neutra_evohka)
                    }
                }
            )
        }
    }.onFailure { error ->
        val diagnostico = describirFalloFotografia(
            estado = null,
            error = error,
            etapaSinRespuestaHttp = "Fallo al iniciar ImageRequest"
        )
        Log.e(
            ETIQUETA_FOTOS,
            "coil_inicio_error diagnostico=$diagnostico"
        )
        setImageResource(R.drawable.ic_foto_neutra_evohka)
    }
}

internal fun ImageView.cargarMiniatura(uri: Uri) {
    setImageResource(R.drawable.ic_foto_neutra_evohka)
    runCatching {
        load(uri, CargadorImagenesEvohka.obtener(context)) {
            crossfade(true)
            listener(
                onError = { _, resultado ->
                    Log.e(
                        ETIQUETA_FOTOS,
                        "coil_miniatura_error " +
                            "error=${resultado.throwable.javaClass.simpleName}"
                    )
                    post {
                        setImageResource(R.drawable.ic_foto_neutra_evohka)
                    }
                }
            )
        }
    }.onFailure { error ->
        Log.e(
            ETIQUETA_FOTOS,
            "coil_miniatura_inicio_error " +
                "error=${error.javaClass.simpleName}"
        )
        setImageResource(R.drawable.ic_foto_neutra_evohka)
    }
}

private const val ETIQUETA_FOTOS = "EvohkaFotos"
private const val MAXIMO_MENSAJE_DIAGNOSTICO = 160

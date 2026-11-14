package com.example.evohkaapp

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

internal data class ResultadoSeleccionFotografias(
    val aceptadas: List<Uri>,
    val descartadas: Int,
    val ultimoMotivo: String?
)

internal object FotografiaMultipart {
    fun validarSeleccion(
        contexto: Context,
        nuevas: List<Uri>,
        actuales: List<Uri>,
        fotografiasExistentes: Int = 0
    ): ResultadoSeleccionFotografias {
        val cupo = PoliticaFotografiasAndroid.cupoDisponible(
            fotografiasExistentes,
            actuales.size
        )
        val conocidas = actuales.mapTo(mutableSetOf()) { it.toString() }
        val aceptadas = mutableListOf<Uri>()
        var descartadas = 0
        var ultimoMotivo: String? = null

        for (uri in nuevas) {
            if (aceptadas.size >= cupo || !conocidas.add(uri.toString())) {
                descartadas += 1
                ultimoMotivo = "El inmueble admite máximo 10 fotografías"
                continue
            }

            val mimeType = runCatching {
                contexto.contentResolver.getType(uri)
            }.getOrNull()
            val tamano = obtenerTamano(contexto.contentResolver, uri)
            val motivo = PoliticaFotografiasAndroid.motivoRechazo(
                mimeType,
                tamano
            )
            if (motivo != null) {
                descartadas += 1
                ultimoMotivo = motivo
            } else {
                aceptadas += uri
            }
        }

        return ResultadoSeleccionFotografias(
            aceptadas = aceptadas,
            descartadas = descartadas,
            ultimoMotivo = ultimoMotivo
        )
    }

    fun crearPartes(
        contexto: Context,
        uris: List<Uri>
    ): List<MultipartBody.Part> =
        uris.mapIndexed { indice, uri ->
            val mimeType = contexto.contentResolver.getType(uri)
                ?: "application/octet-stream"
            val cuerpo = ContenidoUriRequestBody(
                contexto.contentResolver,
                uri,
                mimeType,
                obtenerTamano(contexto.contentResolver, uri)
            )
            MultipartBody.Part.createFormData(
                "fotos",
                "fotografia-${indice + 1}.${extensionPara(mimeType)}",
                cuerpo
            )
        }

    private fun obtenerTamano(
        resolver: ContentResolver,
        uri: Uri
    ): Long? {
        val tamanoConsultado = runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val indice = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (
                    indice >= 0 &&
                    cursor.moveToFirst() &&
                    !cursor.isNull(indice)
                ) {
                    cursor.getLong(indice)
                } else {
                    null
                }
            }
        }.getOrNull()
        if (tamanoConsultado != null) {
            return tamanoConsultado
        }

        return runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it >= 0L }
            }
        }.getOrNull()
    }

    private fun extensionPara(mimeType: String): String =
        when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
}

private class ContenidoUriRequestBody(
    private val resolver: ContentResolver,
    private val uri: Uri,
    mimeType: String,
    private val tamano: Long?
) : RequestBody() {
    private val tipo: MediaType? = mimeType.toMediaTypeOrNull()

    override fun contentType(): MediaType? = tipo

    override fun contentLength(): Long = tamano ?: -1L

    override fun writeTo(sink: BufferedSink) {
        val entrada = checkNotNull(resolver.openInputStream(uri)) {
            "No fue posible abrir la fotografía seleccionada"
        }
        entrada.use { stream ->
            stream.source().use { source ->
                sink.writeAll(source)
            }
        }
    }
}

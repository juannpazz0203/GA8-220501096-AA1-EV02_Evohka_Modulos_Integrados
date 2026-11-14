package com.example.evohkaapp

data class FotoPropiedad(
    val id_foto: Int,
    val id_propiedad: Int,
    val mime_type: String,
    val tamano_bytes: Long,
    val orden: Int,
    val es_portada: Boolean,
    val fecha_creacion: String?,
    val ruta_http: String
)

data class RespuestaFotosPropiedad(
    val mensaje: String,
    val total: Int,
    val datos: List<FotoPropiedad>
)

data class RespuestaFotoPropiedad(
    val mensaje: String,
    val datos: FotoPropiedad?
)

data class ReordenarFotosRequest(
    val orden_fotos: List<Int>,
    val id_portada: Int
)

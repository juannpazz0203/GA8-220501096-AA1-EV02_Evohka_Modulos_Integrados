package com.example.evohkaapp

data class RespuestaMensaje(
    val mensaje: String,
    val datos: Mensaje?
)

data class RespuestaMensajes(
    val mensaje: String,
    val total: Int,
    val datos: List<Mensaje>
)

data class RespuestaConversaciones(
    val mensaje: String,
    val total: Int,
    val datos: List<Conversacion>
)

data class RespuestaOperacionMensajes(
    val mensaje: String,
    val actualizados: Int
)

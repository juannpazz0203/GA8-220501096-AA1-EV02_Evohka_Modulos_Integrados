package com.example.evohkaapp

data class Notificacion(
    val id_notificacion: Int,
    val id_usuario: Int,
    val titulo: String?,
    val mensaje: String?,
    val fecha: String?,
    val leida: Boolean
)

data class RespuestaNotificacion(
    val mensaje: String,
    val datos: Notificacion?
)

data class RespuestaNotificaciones(
    val mensaje: String,
    val total: Int,
    val no_leidas: Int,
    val datos: List<Notificacion>
)

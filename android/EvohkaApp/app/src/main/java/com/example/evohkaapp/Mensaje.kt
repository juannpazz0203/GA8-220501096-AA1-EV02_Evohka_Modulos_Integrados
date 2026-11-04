package com.example.evohkaapp

data class Mensaje(
    val id_mensaje: Int,
    val id_remitente: Int,
    val id_destinatario: Int,
    val id_propiedad: Int,
    val contenido: String,
    val fecha_envio: String?,
    val leido: Boolean
)

data class Conversacion(
    val id_propiedad: Int,
    val titulo_propiedad: String,
    val id_otro_usuario: Int,
    val nombre_otro_usuario: String?,
    val apellido_otro_usuario: String?,
    val ultimo_mensaje: String?,
    val fecha_ultimo_mensaje: String?,
    val no_leidos: Int
)

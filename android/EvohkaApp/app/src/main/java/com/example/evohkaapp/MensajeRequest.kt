package com.example.evohkaapp

data class EnviarMensajeRequest(
    val id_destinatario: Int,
    val id_propiedad: Int,
    val contenido: String
)

data class MarcarMensajesLeidosRequest(
    val id_propiedad: Int,
    val id_usuario: Int
)

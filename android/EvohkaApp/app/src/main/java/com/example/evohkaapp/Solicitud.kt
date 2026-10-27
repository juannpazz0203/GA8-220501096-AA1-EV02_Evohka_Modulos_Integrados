package com.example.evohkaapp

data class Solicitud(
    val id_solicitud: Int,
    val id_usuario: Int,
    val nombre_solicitante: String?,
    val apellido_solicitante: String?,
    val id_propiedad: Int,
    val titulo_propiedad: String?,
    val fecha_solicitud: String?,
    val estado: String?,
    val observaciones: String?
)

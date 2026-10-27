package com.example.evohkaapp

data class CrearSolicitudRequest(
    val id_propiedad: Int
)

data class ActualizarEstadoSolicitudRequest(
    val estado: String
)

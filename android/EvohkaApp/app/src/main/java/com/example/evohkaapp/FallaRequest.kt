package com.example.evohkaapp

data class CrearFallaRequest(
    val id_propiedad: Int,
    val categoria: String,
    val descripcion: String
)

data class ActualizarEstadoFallaRequest(
    val estado: String
)

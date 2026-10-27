package com.example.evohkaapp

data class RespuestaSolicitud(
    val mensaje: String,
    val datos: Solicitud?
)

data class RespuestaSolicitudes(
    val mensaje: String,
    val total: Int,
    val datos: List<Solicitud>
)

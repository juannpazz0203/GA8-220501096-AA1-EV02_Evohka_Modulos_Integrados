package com.example.evohkaapp

data class CrearContratoRequest(
    val id_solicitud: Int,
    val fecha_inicio: String,
    val fecha_fin: String,
    val valor_mensual: Double,
    val deposito: Double?,
    val descripcion: String?
)

data class ActualizarEstadoContratoRequest(
    val estado: String
)

package com.example.evohkaapp

data class RespuestaPago(
    val mensaje: String,
    val datos: Pago?
)

data class RespuestaPagos(
    val mensaje: String,
    val total: Int,
    val datos: List<Pago>
)

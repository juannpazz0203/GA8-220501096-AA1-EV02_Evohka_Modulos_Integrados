package com.example.evohkaapp

data class CrearPagoRequest(
    val id_contrato: Int,
    val fecha_pago: String,
    val monto: Double,
    val metodo_pago: String,
    val observaciones: String?
)

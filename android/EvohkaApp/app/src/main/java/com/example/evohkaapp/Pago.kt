package com.example.evohkaapp

data class Pago(
    val id_pago: Int,
    val id_contrato: Int,
    val fecha_pago: String?,
    val monto: Double?,
    val metodo_pago: String?,
    val estado: String?,
    val observaciones: String?,
    val id_arrendatario: Int,
    val id_propiedad: Int,
    val titulo_propiedad: String?,
    val id_propietario: Int
)

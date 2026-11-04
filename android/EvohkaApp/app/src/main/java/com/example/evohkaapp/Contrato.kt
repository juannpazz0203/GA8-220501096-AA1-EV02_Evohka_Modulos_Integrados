package com.example.evohkaapp

data class Contrato(
    val id_contrato: Int,
    val id_solicitud: Int,
    val fecha_inicio: String?,
    val fecha_fin: String?,
    val valor_mensual: Double?,
    val deposito: Double?,
    val estado: String?,
    val descripcion: String?,
    val id_arrendatario: Int,
    val nombre_arrendatario: String?,
    val apellido_arrendatario: String?,
    val id_propiedad: Int,
    val titulo_propiedad: String?,
    val id_propietario: Int
)

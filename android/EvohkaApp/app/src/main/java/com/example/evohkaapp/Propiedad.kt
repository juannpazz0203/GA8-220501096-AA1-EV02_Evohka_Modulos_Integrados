package com.example.evohkaapp

data class Propiedad(
    val id_propiedad: Int?,
    val titulo: String?,
    val tipo_inmueble: String?,
    val direccion: String?,
    val barrio: String?,
    val ciudad: String?,
    val canon: Double?,
    val habitaciones: Int?,
    val banos: Int?,
    val id_propietario: Int?
)
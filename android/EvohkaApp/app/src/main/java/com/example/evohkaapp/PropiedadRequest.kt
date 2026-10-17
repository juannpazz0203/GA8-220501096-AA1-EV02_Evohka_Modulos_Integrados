package com.example.evohkaapp

data class PropiedadRequest(
    val titulo: String,
    val tipo_inmueble: String,
    val direccion: String,
    val barrio: String,
    val ciudad: String,
    val canon: Double,
    val habitaciones: Int,
    val banos: Int
)
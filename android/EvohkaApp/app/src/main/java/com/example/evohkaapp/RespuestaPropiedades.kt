package com.example.evohkaapp

data class RespuestaPropiedades(
    val mensaje: String,
    val total: Int,
    val datos: List<Propiedad>
)
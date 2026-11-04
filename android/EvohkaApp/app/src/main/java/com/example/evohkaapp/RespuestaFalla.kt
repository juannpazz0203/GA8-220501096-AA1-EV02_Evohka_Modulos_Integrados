package com.example.evohkaapp

data class RespuestaFalla(
    val mensaje: String,
    val datos: Falla?
)

data class RespuestaFallas(
    val mensaje: String,
    val total: Int,
    val datos: List<Falla>
)

data class RespuestaPropiedadesFalla(
    val mensaje: String,
    val total: Int,
    val datos: List<PropiedadFalla>
)

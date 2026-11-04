package com.example.evohkaapp

data class RespuestaContrato(
    val mensaje: String,
    val datos: Contrato?
)

data class RespuestaContratos(
    val mensaje: String,
    val total: Int,
    val datos: List<Contrato>
)

package com.example.evohkaapp

data class RespuestaSesion(
    val ok: Boolean,
    val mensaje: String,
    val usuario: Usuario?
)

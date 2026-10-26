package com.example.evohkaapp

data class RegistroResponse(
    val ok: Boolean,
    val mensaje: String,
    val usuario: Usuario?
)
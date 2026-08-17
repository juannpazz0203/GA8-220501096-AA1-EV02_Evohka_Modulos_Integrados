package com.example.evohkaapp

data class LoginResponse(
    val ok: Boolean,
    val mensaje: String,
    val usuario: Usuario?,
    val token: String?
)
package com.example.evohkaapp

data class RegistroRequest(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val contrasena: String,
    val telefono: String?,
    val rol: String
)
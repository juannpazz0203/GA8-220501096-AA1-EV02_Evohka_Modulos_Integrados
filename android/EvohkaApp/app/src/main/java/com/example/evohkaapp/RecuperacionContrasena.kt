package com.example.evohkaapp

import com.google.gson.annotations.SerializedName

data class SolicitarRecuperacionRequest(
    val correo: String
)

data class RestablecerContrasenaRequest(
    val correo: String,
    val codigo: String,
    @SerializedName("nueva_contrasena")
    val nuevaContrasena: String
)

data class RespuestaRecuperacion(
    val ok: Boolean,
    val mensaje: String
)

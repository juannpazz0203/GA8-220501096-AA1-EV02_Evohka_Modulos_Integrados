package com.example.evohkaapp

data class Falla(
    val id_falla: Int,
    val id_usuario: Int,
    val nombre_arrendatario: String?,
    val apellido_arrendatario: String?,
    val id_propiedad: Int,
    val titulo_propiedad: String?,
    val id_propietario: Int,
    val categoria: String,
    val descripcion: String,
    val fecha_reporte: String?,
    val estado: String
)

data class PropiedadFalla(
    val id_propiedad: Int,
    val titulo: String,
    val direccion: String?,
    val ciudad: String?
)

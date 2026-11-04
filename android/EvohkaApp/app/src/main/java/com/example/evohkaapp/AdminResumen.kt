package com.example.evohkaapp

data class RespuestaAdmin(
    val mensaje: String,
    val datos: AdminResumen?
)

data class AdminResumen(
    val totales: AdminTotales,
    val usuarios: List<AdminUsuario>,
    val propiedades: List<AdminPropiedad>,
    val solicitudes: List<AdminSolicitud>,
    val contratos: List<AdminContrato>,
    val fallas: List<AdminFalla>
)

data class AdminTotales(
    val usuarios: Int,
    val propiedades: Int,
    val solicitudes: Int,
    val contratos: Int,
    val pagos: Int,
    val fallas: Int,
    val mensajes: Int,
    val notificaciones: Int
)

data class AdminUsuario(
    val id_usuario: Int,
    val nombre: String,
    val apellido: String?,
    val correo: String,
    val telefono: String?,
    val rol: String,
    val fecha_registro: String?
)

data class AdminPropiedad(
    val id_propiedad: Int,
    val titulo: String,
    val tipo_inmueble: String?,
    val direccion: String?,
    val ciudad: String?,
    val canon: Double?,
    val id_propietario: Int,
    val nombre_propietario: String?,
    val apellido_propietario: String?
)

data class AdminSolicitud(
    val id_solicitud: Int,
    val fecha_solicitud: String?,
    val estado: String,
    val id_usuario: Int,
    val id_propiedad: Int,
    val titulo_propiedad: String?
)

data class AdminContrato(
    val id_contrato: Int,
    val id_solicitud: Int,
    val fecha_inicio: String?,
    val fecha_fin: String?,
    val valor_mensual: Double?,
    val estado: String,
    val id_arrendatario: Int,
    val id_propiedad: Int,
    val titulo_propiedad: String?,
    val id_propietario: Int
)

data class AdminFalla(
    val id_falla: Int,
    val id_usuario: Int,
    val id_propiedad: Int,
    val titulo_propiedad: String?,
    val categoria: String,
    val descripcion: String,
    val fecha_reporte: String?,
    val estado: String
)

package com.example.evohkaapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/login")
    fun login(
        @Body datos: LoginRequest
    ): Call<LoginResponse>

    @POST("api/auth/registro")
    fun registrarUsuario(
        @Body datos: RegistroRequest
    ): Call<RegistroResponse>

    @GET("api/propiedades")
    fun listarPropiedades(
        @Header("Authorization") token: String
    ): Call<RespuestaPropiedades>

    @POST("api/propiedades")
    fun registrarPropiedad(
        @Header("Authorization") token: String,
        @Body propiedad: PropiedadRequest
    ): Call<RespuestaPropiedad>

    @PUT("api/propiedades/{id}")
    fun actualizarPropiedad(
        @Header("Authorization") token: String,
        @Path("id") idPropiedad: Int,
        @Body propiedad: PropiedadRequest
    ): Call<RespuestaPropiedad>

    @DELETE("api/propiedades/{id}")
    fun eliminarPropiedad(
        @Header("Authorization") token: String,
        @Path("id") idPropiedad: Int
    ): Call<RespuestaPropiedad>

    @POST("api/solicitudes")
    fun crearSolicitud(
        @Header("Authorization") token: String,
        @Body solicitud: CrearSolicitudRequest
    ): Call<RespuestaSolicitud>

    @GET("api/solicitudes/recibidas")
    fun listarSolicitudesRecibidas(
        @Header("Authorization") token: String
    ): Call<RespuestaSolicitudes>

    @PATCH("api/solicitudes/{id}/estado")
    fun actualizarEstadoSolicitud(
        @Header("Authorization") token: String,
        @Path("id") idSolicitud: Int,
        @Body estado: ActualizarEstadoSolicitudRequest
    ): Call<RespuestaSolicitud>

    @GET("api/contratos")
    fun listarContratos(
        @Header("Authorization") token: String
    ): Call<RespuestaContratos>

    @POST("api/contratos")
    fun crearContrato(
        @Header("Authorization") token: String,
        @Body contrato: CrearContratoRequest
    ): Call<RespuestaContrato>

    @PATCH("api/contratos/{id}/estado")
    fun actualizarEstadoContrato(
        @Header("Authorization") token: String,
        @Path("id") idContrato: Int,
        @Body estado: ActualizarEstadoContratoRequest
    ): Call<RespuestaContrato>

    @GET("api/pagos")
    fun listarPagos(
        @Header("Authorization") token: String,
        @Query("contrato") idContrato: Int? = null
    ): Call<RespuestaPagos>

    @POST("api/pagos")
    fun registrarPago(
        @Header("Authorization") token: String,
        @Body pago: CrearPagoRequest
    ): Call<RespuestaPago>

    @GET("api/reportes/ingresos")
    fun obtenerReporteIngresos(
        @Header("Authorization") token: String
    ): Call<RespuestaReporteIngresos>

    @GET("api/notificaciones")
    fun listarNotificaciones(
        @Header("Authorization") token: String
    ): Call<RespuestaNotificaciones>

    @PATCH("api/notificaciones/{id}/leida")
    fun marcarNotificacionLeida(
        @Header("Authorization") token: String,
        @Path("id") idNotificacion: Int
    ): Call<RespuestaNotificacion>

    @GET("api/fallas")
    fun listarFallas(
        @Header("Authorization") token: String
    ): Call<RespuestaFallas>

    @GET("api/fallas/propiedades")
    fun listarPropiedadesParaFallas(
        @Header("Authorization") token: String
    ): Call<RespuestaPropiedadesFalla>

    @POST("api/fallas")
    fun crearFalla(
        @Header("Authorization") token: String,
        @Body falla: CrearFallaRequest
    ): Call<RespuestaFalla>

    @PATCH("api/fallas/{id}/estado")
    fun actualizarEstadoFalla(
        @Header("Authorization") token: String,
        @Path("id") idFalla: Int,
        @Body estado: ActualizarEstadoFallaRequest
    ): Call<RespuestaFalla>

    @GET("api/mensajes/conversaciones")
    fun listarConversaciones(
        @Header("Authorization") token: String
    ): Call<RespuestaConversaciones>

    @GET("api/mensajes")
    fun listarMensajes(
        @Header("Authorization") token: String,
        @Query("propiedad") idPropiedad: Int,
        @Query("usuario") idUsuario: Int
    ): Call<RespuestaMensajes>

    @POST("api/mensajes")
    fun enviarMensaje(
        @Header("Authorization") token: String,
        @Body mensaje: EnviarMensajeRequest
    ): Call<RespuestaMensaje>

    @PATCH("api/mensajes/leidos")
    fun marcarMensajesLeidos(
        @Header("Authorization") token: String,
        @Body datos: MarcarMensajesLeidosRequest
    ): Call<RespuestaOperacionMensajes>

    @GET("api/admin/resumen")
    fun obtenerResumenAdministrador(
        @Header("Authorization") token: String
    ): Call<RespuestaAdmin>

    @GET("api/admin/usuarios")
    fun listarUsuariosAdministrador(
        @Header("Authorization") token: String
    ): Call<RespuestaUsuariosAdmin>

    @POST("api/admin/usuarios")
    fun crearUsuarioAdministrador(
        @Header("Authorization") token: String,
        @Body usuario: CrearUsuarioAdminRequest
    ): Call<RespuestaUsuarioAdmin>

    @PUT("api/admin/usuarios/{id}")
    fun actualizarUsuarioAdministrador(
        @Header("Authorization") token: String,
        @Path("id") idUsuario: Int,
        @Body usuario: ActualizarUsuarioAdminRequest
    ): Call<RespuestaUsuarioAdmin>

    @DELETE("api/admin/usuarios/{id}")
    fun eliminarUsuarioAdministrador(
        @Header("Authorization") token: String,
        @Path("id") idUsuario: Int
    ): Call<RespuestaUsuarioAdmin>
}

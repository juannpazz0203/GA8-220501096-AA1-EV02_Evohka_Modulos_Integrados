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
}

package com.example.evohkaapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT

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
}

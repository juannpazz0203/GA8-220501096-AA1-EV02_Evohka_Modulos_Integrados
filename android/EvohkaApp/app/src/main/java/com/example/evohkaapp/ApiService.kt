package com.example.evohkaapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/login")
    fun login(
        @Body datos: LoginRequest
    ): Call<LoginResponse>

    @GET("api/propiedades")
    fun listarPropiedades(
        @Header("Authorization") token: String
    ): Call<RespuestaPropiedades>

    @POST("api/propiedades")
    fun registrarPropiedad(
        @Body propiedad: PropiedadRequest
    ): Call<RespuestaPropiedad>
}
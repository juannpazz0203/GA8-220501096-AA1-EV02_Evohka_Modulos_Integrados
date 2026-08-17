package com.example.evohkaapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/login")
    fun login(
        @Body datos: LoginRequest
    ): Call<LoginResponse>
}
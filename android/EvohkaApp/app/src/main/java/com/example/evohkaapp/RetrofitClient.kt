package com.example.evohkaapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient

object RetrofitClient {

    val apiService: ApiService by lazy {

        val clienteHttp = OkHttpClient.Builder()
            .addInterceptor(SesionHttpInterceptor())
            .build()

        Retrofit.Builder()
            .baseUrl(BackendUrlProvider.obtenerBaseUrl())
            .client(clienteHttp)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }
}

package com.example.aapdasetu.data.api

import com.example.aapdasetu.data.model.AuthResponse
import com.example.aapdasetu.data.model.LoginRequest
import com.example.aapdasetu.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AapdaSetuApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<AuthResponse>

    @GET("api/v1/auth/logout")
    suspend fun logout(): Response<AuthResponse>

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, Any>>
}

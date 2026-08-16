package com.example.aapdasetu.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 10.0.2.2 points to localhost on the host computer from the Android Emulator.
    // For physical devices, set this to your computer's local Wi-Fi IP (e.g. "http://192.168.1.X:3000/")
    var baseUrl: String = "http://10.0.2.2:3000/"

    private var authToken: String? = null

    fun setToken(token: String?) {
        authToken = token
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        authToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var retrofitInstance: Retrofit? = null

    fun getApiService(): AapdaSetuApiService {
        if (retrofitInstance == null || retrofitInstance?.baseUrl().toString() != baseUrl) {
            retrofitInstance = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!.create(AapdaSetuApiService::class.java)
    }
}

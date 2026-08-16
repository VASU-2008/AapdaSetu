package com.example.aapdasetu.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class RegisterRequest(
    @SerializedName("fullName")
    val fullName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class UserModel(
    @SerializedName("id")
    val id: String?,
    @SerializedName("username")
    val username: String?,
    @SerializedName("email")
    val email: String?
)

data class AuthResponse(
    @SerializedName("message")
    val message: String?,
    @SerializedName("user")
    val user: UserModel?,
    @SerializedName("token")
    val token: String?
)

data class ErrorResponse(
    @SerializedName("message")
    val message: String?
)

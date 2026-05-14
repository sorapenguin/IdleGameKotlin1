package com.example.idlegameapi.dto.response

data class AuthResponse(
    val token: String,
    val userId: Long,
    val username: String
)

package com.example.idlegameapi.dto.response

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

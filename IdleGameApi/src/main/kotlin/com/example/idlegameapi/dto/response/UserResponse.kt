package com.example.idlegameapi.dto.response

import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val createdAt: LocalDateTime
)

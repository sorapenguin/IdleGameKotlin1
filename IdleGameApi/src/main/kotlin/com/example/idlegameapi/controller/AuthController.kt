package com.example.idlegameapi.controller

import com.example.idlegameapi.dto.request.LoginRequest
import com.example.idlegameapi.dto.request.RegisterRequest
import com.example.idlegameapi.dto.response.ApiResponse
import com.example.idlegameapi.dto.response.AuthResponse
import com.example.idlegameapi.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/cloud-save")
    fun cloudSave(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val result = authService.cloudSave(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, message = "Cloud save created successfully", data = result))
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val result = authService.login(request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Login successful", data = result)
        )
    }
}

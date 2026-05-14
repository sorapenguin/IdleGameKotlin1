package com.example.idlegameapi.controller

import com.example.idlegameapi.dto.response.ApiResponse
import com.example.idlegameapi.dto.response.UserResponse
import com.example.idlegameapi.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.getUserById(id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "User found", data = user)
        )
    }
}

package com.example.idlegameapi.service

import com.example.idlegameapi.dto.response.UserResponse
import com.example.idlegameapi.exception.ResourceNotFoundException
import com.example.idlegameapi.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: UserRepository) {

    fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("User not found with id: $id") }
        return UserResponse(
            id = user.id,
            username = user.username,
            createdAt = user.createdAt
        )
    }
}

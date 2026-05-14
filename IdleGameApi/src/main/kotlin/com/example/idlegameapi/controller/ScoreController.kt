package com.example.idlegameapi.controller

import com.example.idlegameapi.dto.request.ScoreUpdateRequest
import com.example.idlegameapi.dto.response.ApiResponse
import com.example.idlegameapi.dto.response.ScoreResponse
import com.example.idlegameapi.service.ScoreService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/score")
class ScoreController(private val scoreService: ScoreService) {

    @PostMapping("/update")
    fun updateScore(
        authentication: Authentication,
        @Valid @RequestBody request: ScoreUpdateRequest
    ): ResponseEntity<ApiResponse<ScoreResponse>> {
        val score = scoreService.updateScore(authentication.name, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Score updated", data = score)
        )
    }
}

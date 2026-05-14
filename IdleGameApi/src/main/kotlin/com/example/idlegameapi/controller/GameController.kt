package com.example.idlegameapi.controller

import com.example.idlegameapi.dto.request.GameSaveRequest
import com.example.idlegameapi.dto.response.ApiResponse
import com.example.idlegameapi.dto.response.GameStateResponse
import com.example.idlegameapi.service.GameService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/game")
class GameController(private val gameService: GameService) {

    @GetMapping("/state")
    fun getGameState(authentication: Authentication): ResponseEntity<ApiResponse<GameStateResponse>> {
        val state = gameService.getGameState(authentication.name)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Game state retrieved", data = state)
        )
    }

    @PostMapping("/save")
    fun saveGameState(
        authentication: Authentication,
        @Valid @RequestBody request: GameSaveRequest
    ): ResponseEntity<ApiResponse<GameStateResponse>> {
        val state = gameService.saveGameState(authentication.name, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Game state saved", data = state)
        )
    }
}

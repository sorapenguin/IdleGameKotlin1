package com.example.idlegameapi.repository

import com.example.idlegameapi.entity.GameState
import com.example.idlegameapi.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface GameStateRepository : JpaRepository<GameState, Long> {
    fun findByUser(user: User): GameState?
}

package com.example.idlegameapi.repository

import com.example.idlegameapi.entity.Score
import com.example.idlegameapi.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface ScoreRepository : JpaRepository<Score, Long> {
    fun findByUser(user: User): Score?
}

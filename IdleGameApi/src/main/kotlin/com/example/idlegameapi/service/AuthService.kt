package com.example.idlegameapi.service

import com.example.idlegameapi.dto.request.LoginRequest
import com.example.idlegameapi.dto.request.RegisterRequest
import com.example.idlegameapi.dto.response.AuthResponse
import com.example.idlegameapi.entity.GameState
import com.example.idlegameapi.entity.Score
import com.example.idlegameapi.entity.User
import com.example.idlegameapi.exception.InvalidCredentialsException
import com.example.idlegameapi.repository.GameStateRepository
import com.example.idlegameapi.repository.ScoreRepository
import com.example.idlegameapi.repository.UserRepository
import com.example.idlegameapi.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val gameStateRepository: GameStateRepository,
    private val scoreRepository: ScoreRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    @Transactional
    fun cloudSave(request: RegisterRequest): AuthResponse {
        val username = generateUniqueUsername()
        val user = userRepository.save(
            User(
                username = username,
                passwordHash = passwordEncoder.encode(request.password)
            )
        )
        gameStateRepository.save(GameState(user = user))
        scoreRepository.save(Score(user = user))

        return AuthResponse(
            token = jwtService.generateToken(user.username),
            userId = user.id,
            username = user.username
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByUsername(request.username)
            ?: throw InvalidCredentialsException("Invalid username or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid username or password")
        }

        return AuthResponse(
            token = jwtService.generateToken(user.username),
            userId = user.id,
            username = user.username
        )
    }

    private fun generateUniqueUsername(): String {
        val adj = ADJECTIVES.random()
        val noun = NOUNS.random()
        val base = "${adj}_${noun}"
        if (!userRepository.existsByUsername(base)) return base

        repeat(9) {
            val suffix = (1..999).random().toString().padStart(3, '0')
            val candidate = "${base}_${suffix}"
            if (!userRepository.existsByUsername(candidate)) return candidate
        }
        error("Failed to generate unique username")
    }

    companion object {
        private val ADJECTIVES = listOf(
            "swift", "brave", "mighty", "fierce", "silent",
            "crystal", "iron", "ember", "golden", "ancient",
            "cosmic", "wild", "noble", "nimble", "frost",
            "thunder", "flame", "arcane", "lunar", "solar",
            "scarlet", "azure", "jade", "crimson", "silver",
            "shadow", "radiant", "serene", "bold", "phantom"
        )
        private val NOUNS = listOf(
            "dragon", "knight", "archer", "wizard", "phoenix",
            "hunter", "titan", "warrior", "guardian", "ranger",
            "paladin", "rogue", "sentinel", "champion", "striker",
            "blade", "warden", "raider", "slayer", "vanguard",
            "spirit", "tempest", "falcon", "wolf", "bear",
            "eagle", "hawk", "sage", "valor", "phantom"
        )
    }
}

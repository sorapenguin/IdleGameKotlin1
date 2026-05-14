package com.example.idlegameapi.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${app.jwt.secret}")
    private lateinit var secretKeyString: String

    @Value("\${app.jwt.expiration}")
    private var expiration: Long = 86400000L

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKeyString.toByteArray(Charsets.UTF_8))
    }

    fun generateToken(username: String): String =
        Jwts.builder()
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey)
            .compact()

    fun extractUsername(token: String): String = getClaims(token).subject

    fun isTokenValid(token: String): Boolean = try {
        getClaims(token).expiration.after(Date())
    } catch (e: Exception) {
        false
    }

    private fun getClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
}

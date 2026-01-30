package com.aletheia.pros.api.security

import com.aletheia.pros.domain.common.UserId
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SecurityException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/**
 * JWT Token Provider for generating and validating JWT tokens.
 */
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret:default-secret-key-that-should-be-changed-in-production-environment-32chars}")
    private val secretKeyString: String,

    @Value("\${jwt.expiration-ms:86400000}")
    private val expirationMs: Long // Default: 24 hours
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKeyString.toByteArray())
    }

    /**
     * Generates a JWT token for the given user.
     */
    fun generateToken(userId: UserId, email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(userId.value.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    /**
     * Extracts the user ID from the token.
     */
    fun getUserIdFromToken(token: String): UserId {
        val claims = getClaims(token)
        return UserId(UUID.fromString(claims.subject))
    }

    /**
     * Extracts the email from the token.
     */
    fun getEmailFromToken(token: String): String {
        val claims = getClaims(token)
        return claims["email"] as String
    }

    /**
     * Validates the token.
     */
    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: MalformedJwtException) {
            false
        } catch (e: ExpiredJwtException) {
            false
        } catch (e: UnsupportedJwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}

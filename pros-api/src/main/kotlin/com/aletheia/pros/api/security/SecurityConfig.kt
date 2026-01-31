package com.aletheia.pros.api.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Security configuration for the API.
 *
 * Configures:
 * - JWT-based stateless authentication for API endpoints
 * - OAuth2 social login (Google, GitHub) - optional if configured
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Autowired(required = false)
    private var oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler? = null

    @Autowired(required = false)
    private var oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler? = null

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/v1/auth/**").permitAll()
                    .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // All other endpoints require authentication
                    .anyRequest().authenticated()
            }

        // OAuth2 Login configuration (optional)
        if (oAuth2AuthenticationSuccessHandler != null && oAuth2AuthenticationFailureHandler != null) {
            http.oauth2Login { oauth2 ->
                oauth2
                    .authorizationEndpoint { it.baseUri("/oauth2/authorize") }
                    .redirectionEndpoint { it.baseUri("/oauth2/callback/*") }
                    .successHandler(oAuth2AuthenticationSuccessHandler!!)
                    .failureHandler(oAuth2AuthenticationFailureHandler!!)
            }
        }

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}

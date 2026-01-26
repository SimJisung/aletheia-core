package com.aletheia.pros.infrastructure.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * JPA Configuration for the PROS application.
 *
 * Configures:
 * - Entity scanning
 * - Repository scanning
 * - Transaction management
 */
@Configuration
@EntityScan(basePackages = ["com.aletheia.pros.infrastructure.persistence.entity"])
@EnableJpaRepositories(basePackages = ["com.aletheia.pros.infrastructure.persistence.repository"])
@EnableTransactionManagement
class JpaConfig

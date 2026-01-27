package com.aletheia.pros.infrastructure.config

import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.application.port.output.EmotionAnalysisPort
import com.aletheia.pros.application.usecase.decision.CreateDecisionUseCase
import com.aletheia.pros.application.usecase.decision.UserSettingsProvider
import com.aletheia.pros.application.usecase.fragment.CreateFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.DeleteFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.QueryFragmentUseCase
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.value.ValueGraphRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for registering Use Case beans.
 *
 * This wires the application layer use cases with their required dependencies.
 */
@Configuration
class UseCaseConfig {

    @Bean
    fun createFragmentUseCase(
        fragmentRepository: FragmentRepository,
        emotionAnalysisPort: EmotionAnalysisPort,
        embeddingPort: EmbeddingPort
    ): CreateFragmentUseCase {
        return CreateFragmentUseCase(
            fragmentRepository = fragmentRepository,
            emotionAnalysisPort = emotionAnalysisPort,
            embeddingPort = embeddingPort
        )
    }

    @Bean
    fun queryFragmentUseCase(
        fragmentRepository: FragmentRepository,
        embeddingPort: EmbeddingPort
    ): QueryFragmentUseCase {
        return QueryFragmentUseCase(
            fragmentRepository = fragmentRepository,
            embeddingPort = embeddingPort
        )
    }

    @Bean
    fun deleteFragmentUseCase(
        fragmentRepository: FragmentRepository
    ): DeleteFragmentUseCase {
        return DeleteFragmentUseCase(
            fragmentRepository = fragmentRepository
        )
    }

    @Bean
    fun createDecisionUseCase(
        decisionRepository: DecisionRepository,
        fragmentRepository: FragmentRepository,
        valueGraphRepository: ValueGraphRepository,
        embeddingPort: EmbeddingPort,
        userSettingsProvider: UserSettingsProvider
    ): CreateDecisionUseCase {
        return CreateDecisionUseCase(
            decisionRepository = decisionRepository,
            fragmentRepository = fragmentRepository,
            valueGraphRepository = valueGraphRepository,
            embeddingPort = embeddingPort,
            userSettingsProvider = userSettingsProvider
        )
    }
}

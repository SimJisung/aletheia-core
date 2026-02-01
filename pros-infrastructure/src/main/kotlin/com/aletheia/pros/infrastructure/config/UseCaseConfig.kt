package com.aletheia.pros.infrastructure.config

import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.application.port.output.EmotionAnalysisPort
import com.aletheia.pros.application.port.output.ExplanationPort
import com.aletheia.pros.application.port.output.ValueExtractionPort
import com.aletheia.pros.application.usecase.auth.AuthUseCase
import com.aletheia.pros.application.usecase.auth.OAuthUseCase
import com.aletheia.pros.application.usecase.auth.PasswordEncoderPort
import com.aletheia.pros.application.usecase.user.GetCurrentUserUseCase
import com.aletheia.pros.domain.user.OAuthAccountRepository
import com.aletheia.pros.application.usecase.decision.CreateDecisionUseCase
import com.aletheia.pros.application.usecase.decision.GetDecisionExplanationUseCase
import com.aletheia.pros.application.usecase.decision.QueryDecisionUseCase
import com.aletheia.pros.application.usecase.decision.SubmitFeedbackUseCase
import com.aletheia.pros.application.usecase.decision.UserSettingsProvider
import com.aletheia.pros.application.usecase.fragment.CreateFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.DeleteFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.QueryFragmentUseCase
import com.aletheia.pros.application.usecase.value.GetValueImportanceUseCase
import com.aletheia.pros.application.usecase.value.QueryValueGraphUseCase
import com.aletheia.pros.application.usecase.value.SetValueImportanceUseCase
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.user.UserRepository
import com.aletheia.pros.domain.value.ValueGraphRepository
import com.aletheia.pros.domain.value.ValueImportanceRepository
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
        embeddingPort: EmbeddingPort,
        valueExtractionPort: ValueExtractionPort,
        valueGraphRepository: ValueGraphRepository
    ): CreateFragmentUseCase {
        return CreateFragmentUseCase(
            fragmentRepository = fragmentRepository,
            emotionAnalysisPort = emotionAnalysisPort,
            embeddingPort = embeddingPort,
            valueExtractionPort = valueExtractionPort,
            valueGraphRepository = valueGraphRepository
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
        valueImportanceRepository: ValueImportanceRepository,
        embeddingPort: EmbeddingPort,
        userSettingsProvider: UserSettingsProvider
    ): CreateDecisionUseCase {
        return CreateDecisionUseCase(
            decisionRepository = decisionRepository,
            fragmentRepository = fragmentRepository,
            valueGraphRepository = valueGraphRepository,
            valueImportanceRepository = valueImportanceRepository,
            embeddingPort = embeddingPort,
            userSettingsProvider = userSettingsProvider
        )
    }

    @Bean
    fun getDecisionExplanationUseCase(
        decisionRepository: DecisionRepository,
        fragmentRepository: FragmentRepository,
        explanationPort: ExplanationPort
    ): GetDecisionExplanationUseCase {
        return GetDecisionExplanationUseCase(
            decisionRepository = decisionRepository,
            fragmentRepository = fragmentRepository,
            explanationPort = explanationPort
        )
    }

    @Bean
    fun queryValueGraphUseCase(
        valueGraphRepository: ValueGraphRepository,
        fragmentRepository: FragmentRepository
    ): QueryValueGraphUseCase {
        return QueryValueGraphUseCase(
            valueGraphRepository = valueGraphRepository,
            fragmentRepository = fragmentRepository
        )
    }

    @Bean
    fun queryDecisionUseCase(
        decisionRepository: DecisionRepository
    ): QueryDecisionUseCase {
        return QueryDecisionUseCase(
            decisionRepository = decisionRepository
        )
    }

    @Bean
    fun submitFeedbackUseCase(
        decisionRepository: DecisionRepository,
        userSettingsProvider: UserSettingsProvider
    ): SubmitFeedbackUseCase {
        return SubmitFeedbackUseCase(
            decisionRepository = decisionRepository,
            userSettingsProvider = userSettingsProvider
        )
    }

    @Bean
    fun authUseCase(
        userRepository: UserRepository,
        passwordEncoderPort: PasswordEncoderPort
    ): AuthUseCase {
        return AuthUseCase(
            userRepository = userRepository,
            passwordEncoder = passwordEncoderPort
        )
    }

    @Bean
    fun oauthUseCase(
        userRepository: UserRepository,
        oauthAccountRepository: OAuthAccountRepository
    ): OAuthUseCase {
        return OAuthUseCase(
            userRepository = userRepository,
            oauthAccountRepository = oauthAccountRepository
        )
    }

    @Bean
    fun getCurrentUserUseCase(
        userRepository: UserRepository
    ): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(
            userRepository = userRepository
        )
    }

    @Bean
    fun setValueImportanceUseCase(
        valueImportanceRepository: ValueImportanceRepository
    ): SetValueImportanceUseCase {
        return SetValueImportanceUseCase(
            repository = valueImportanceRepository
        )
    }

    @Bean
    fun getValueImportanceUseCase(
        valueImportanceRepository: ValueImportanceRepository
    ): GetValueImportanceUseCase {
        return GetValueImportanceUseCase(
            repository = valueImportanceRepository
        )
    }
}

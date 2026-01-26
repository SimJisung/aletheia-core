package com.aletheia.pros.infrastructure.persistence.mapper

import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FeedbackId
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.*
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.infrastructure.persistence.entity.DecisionEntity
import com.aletheia.pros.infrastructure.persistence.entity.DecisionFeedbackEntity
import org.springframework.stereotype.Component

/**
 * Mapper for converting between Decision domain models and JPA entities.
 */
@Component
class DecisionMapper {

    // ==================== Decision Mapping ====================

    /**
     * Converts a domain Decision to a JPA entity.
     */
    fun toEntity(domain: Decision): DecisionEntity {
        return DecisionEntity(
            id = domain.id.value,
            userId = domain.userId.value,
            title = domain.title,
            optionA = domain.optionA,
            optionB = domain.optionB,
            priorityAxis = domain.priorityAxis?.name,
            probabilityA = domain.result.probabilityA.value,
            probabilityB = domain.result.probabilityB.value,
            regretRiskA = domain.result.regretRiskA.value,
            regretRiskB = domain.result.regretRiskB.value,
            evidenceFragmentIds = domain.result.evidenceFragmentIds
                .map { it.value }
                .toTypedArray(),
            valueAlignment = domain.result.valueAlignment
                .mapKeys { it.key.name },
            createdAt = domain.createdAt
        )
    }

    /**
     * Converts a JPA DecisionEntity to a domain model.
     */
    fun toDomain(entity: DecisionEntity): Decision {
        val result = DecisionResult(
            probabilityA = Probability(entity.probabilityA),
            probabilityB = Probability(entity.probabilityB),
            regretRiskA = RegretRisk(entity.regretRiskA),
            regretRiskB = RegretRisk(entity.regretRiskB),
            evidenceFragmentIds = entity.evidenceFragmentIds
                .map { FragmentId(it) },
            valueAlignment = entity.valueAlignment
                .mapKeys { ValueAxis.valueOf(it.key) }
        )

        return Decision(
            id = DecisionId(entity.id),
            userId = UserId(entity.userId),
            title = entity.title,
            optionA = entity.optionA,
            optionB = entity.optionB,
            priorityAxis = entity.priorityAxis?.let { ValueAxis.valueOf(it) },
            result = result,
            createdAt = entity.createdAt
        )
    }

    /**
     * Converts a list of decision entities to domain models.
     */
    fun toDomainList(entities: List<DecisionEntity>): List<Decision> {
        return entities.map { toDomain(it) }
    }

    // ==================== DecisionFeedback Mapping ====================

    /**
     * Converts a domain DecisionFeedback to a JPA entity.
     */
    fun toEntity(domain: DecisionFeedback, decisionEntity: DecisionEntity): DecisionFeedbackEntity {
        return DecisionFeedbackEntity(
            id = domain.id.value,
            decision = decisionEntity,
            feedbackType = domain.feedbackType.name,
            createdAt = domain.createdAt
        )
    }

    /**
     * Converts a JPA DecisionFeedbackEntity to a domain model.
     */
    fun toDomain(entity: DecisionFeedbackEntity): DecisionFeedback {
        return DecisionFeedback(
            id = FeedbackId(entity.id),
            decisionId = DecisionId(entity.decision.id),
            feedbackType = FeedbackType.valueOf(entity.feedbackType),
            createdAt = entity.createdAt
        )
    }

    /**
     * Converts a list of feedback entities to domain models.
     */
    fun toFeedbackDomainList(entities: List<DecisionFeedbackEntity>): List<DecisionFeedback> {
        return entities.map { toDomain(it) }
    }
}

package com.aletheia.pros.infrastructure.persistence.adapter

import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FeedbackId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.*
import com.aletheia.pros.infrastructure.persistence.mapper.DecisionMapper
import com.aletheia.pros.infrastructure.persistence.repository.JpaDecisionFeedbackRepository
import com.aletheia.pros.infrastructure.persistence.repository.JpaDecisionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * Adapter implementing DecisionRepository using JPA.
 */
@Repository
@Transactional
class DecisionRepositoryAdapter(
    private val decisionRepository: JpaDecisionRepository,
    private val feedbackRepository: JpaDecisionFeedbackRepository,
    private val mapper: DecisionMapper
) : DecisionRepository {

    companion object {
        // Feedback window: 24-72 hours after decision
        private val FEEDBACK_MIN_DELAY = Duration.ofHours(24)
        private val FEEDBACK_MAX_DELAY = Duration.ofHours(72)
    }

    // ==================== Decision Operations ====================

    override fun save(decision: Decision): Decision {
        val entity = mapper.toEntity(decision)
        val saved = decisionRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findById(id: DecisionId): Decision? {
        return decisionRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByUserId(userId: UserId, limit: Int, offset: Int): List<Decision> {
        val pageNumber = offset / limit
        val offsetInPage = offset % limit
        val pageSize = limit + offsetInPage
        val pageable = PageRequest.of(pageNumber, pageSize)
        val page = decisionRepository.findByUserId(userId.value, pageable)
        val entities = page.content.drop(offsetInPage)
        return mapper.toDomainList(entities)
    }

    @Transactional(readOnly = true)
    override fun findByUserIdAndTimeRange(
        userId: UserId,
        from: Instant,
        to: Instant
    ): List<Decision> {
        val entities = decisionRepository.findByUserIdAndTimeRange(userId.value, from, to)
        return mapper.toDomainList(entities)
    }

    @Transactional(readOnly = true)
    override fun countByUserId(userId: UserId): Long {
        return decisionRepository.countByUserId(userId.value)
    }

    // ==================== Feedback Operations ====================

    override fun saveFeedback(feedback: DecisionFeedback): DecisionFeedback {
        val decisionEntity = decisionRepository.findById(feedback.decisionId.value)
            .orElseThrow { IllegalArgumentException("Decision not found: ${feedback.decisionId}") }

        val entity = mapper.toEntity(feedback, decisionEntity)
        val saved = feedbackRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findFeedbackById(id: FeedbackId): DecisionFeedback? {
        return feedbackRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findFeedbackByDecisionId(decisionId: DecisionId): DecisionFeedback? {
        return feedbackRepository.findByDecisionId(decisionId.value)
            ?.let { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findFeedbacksByUserId(userId: UserId): List<DecisionFeedback> {
        return mapper.toFeedbackDomainList(feedbackRepository.findByUserId(userId.value))
    }

    @Transactional(readOnly = true)
    override fun hasFeedback(decisionId: DecisionId): Boolean {
        return feedbackRepository.existsByDecisionId(decisionId.value)
    }

    // ==================== Analytics Operations ====================

    @Transactional(readOnly = true)
    override fun findDecisionsNeedingFeedback(userId: UserId): List<Decision> {
        val now = Instant.now()
        val minTime = now.minus(FEEDBACK_MAX_DELAY)
        val maxTime = now.minus(FEEDBACK_MIN_DELAY)

        val entities = decisionRepository.findDecisionsNeedingFeedback(
            userId.value,
            minTime,
            maxTime
        )
        return mapper.toDomainList(entities)
    }

    @Transactional(readOnly = true)
    override fun getFeedbackStats(userId: UserId): FeedbackStats {
        val totalDecisions = decisionRepository.countByUserId(userId.value).toInt()
        val feedbackCounts = feedbackRepository.countByFeedbackTypeForUser(userId.value)

        var satisfiedCount = 0
        var neutralCount = 0
        var regretCount = 0

        for (row in feedbackCounts) {
            val type = row[0] as String
            val count = (row[1] as Number).toInt()
            when (type) {
                "SATISFIED" -> satisfiedCount = count
                "NEUTRAL" -> neutralCount = count
                "REGRET" -> regretCount = count
            }
        }

        val totalWithFeedback = satisfiedCount + neutralCount + regretCount

        return FeedbackStats(
            totalDecisions = totalDecisions,
            totalWithFeedback = totalWithFeedback,
            satisfiedCount = satisfiedCount,
            neutralCount = neutralCount,
            regretCount = regretCount
        )
    }
}

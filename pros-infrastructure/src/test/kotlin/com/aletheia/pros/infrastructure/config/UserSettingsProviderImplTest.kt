package com.aletheia.pros.infrastructure.config

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.infrastructure.persistence.entity.UserSettingsEntity
import com.aletheia.pros.infrastructure.persistence.repository.JpaUserSettingsRepository
import io.mockk.any
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

@ExtendWith(MockKExtension::class)
@DisplayName("UserSettingsProviderImpl Tests")
class UserSettingsProviderImplTest {

    @MockK
    private lateinit var repository: JpaUserSettingsRepository

    private lateinit var provider: UserSettingsProviderImpl

    @BeforeEach
    fun setUp() {
        provider = UserSettingsProviderImpl(
            repository = repository,
            defaultLambda = 1.0,
            defaultRegretPrior = 0.2
        )
    }

    @Test
    fun `should return existing settings when concurrent create hits unique constraint`() {
        val userId = UserId.generate()
        val existing = UserSettingsEntity(
            id = UUID.randomUUID(),
            userId = userId.value,
            lambda = 1.7,
            regretPrior = 0.35,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { repository.findByUserId(userId.value) } returns null andThen existing
        every { repository.save(any()) } throws DataIntegrityViolationException("duplicate user_id")

        val settings = runSuspend { provider.getSettings(userId) }

        assertThat(settings.lambda).isEqualTo(existing.lambda)
        assertThat(settings.regretPrior).isEqualTo(existing.regretPrior)

        verify(exactly = 1) { repository.save(any()) }
        verify(exactly = 2) { repository.findByUserId(userId.value) }
        confirmVerified(repository)
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(res: Result<T>) {
                result = res
            }
        })
        return result!!.getOrThrow()
    }
}

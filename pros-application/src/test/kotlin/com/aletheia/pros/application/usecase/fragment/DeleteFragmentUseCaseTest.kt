package com.aletheia.pros.application.usecase.fragment

import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.fragment.FragmentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@DisplayName("DeleteFragmentUseCase Tests")
class DeleteFragmentUseCaseTest {

    @MockK
    private lateinit var fragmentRepository: FragmentRepository

    private lateinit var useCase: DeleteFragmentUseCase

    @BeforeEach
    fun setUp() {
        useCase = DeleteFragmentUseCase(
            fragmentRepository = fragmentRepository
        )
    }

    @Nested
    @DisplayName("Successful Soft Delete")
    inner class SuccessfulDelete {

        @Test
        fun `should soft delete fragment successfully`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()
            coEvery { fragmentRepository.softDelete(fragmentId) } returns true

            // When
            val result = useCase.execute(fragmentId)

            // Then
            assertThat(result).isTrue()
            coVerify(exactly = 1) { fragmentRepository.softDelete(fragmentId) }
        }

        @Test
        fun `should return true when fragment is deleted`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()
            coEvery { fragmentRepository.softDelete(fragmentId) } returns true

            // When
            val result = useCase.execute(fragmentId)

            // Then
            assertThat(result).isTrue()
        }
    }

    @Nested
    @DisplayName("Fragment Not Found or Already Deleted")
    inner class NotFoundOrDeleted {

        @Test
        fun `should return false when fragment not found`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()
            coEvery { fragmentRepository.softDelete(fragmentId) } returns false

            // When
            val result = useCase.execute(fragmentId)

            // Then
            assertThat(result).isFalse()
        }

        @Test
        fun `should return false when fragment already deleted`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()
            coEvery { fragmentRepository.softDelete(fragmentId) } returns false

            // When
            val result = useCase.execute(fragmentId)

            // Then
            assertThat(result).isFalse()
        }

        @Test
        fun `should be idempotent - multiple deletes on same fragment`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()
            coEvery { fragmentRepository.softDelete(fragmentId) } returns true andThen false

            // When - first delete
            val firstResult = useCase.execute(fragmentId)

            // When - second delete
            val secondResult = useCase.execute(fragmentId)

            // Then
            assertThat(firstResult).isTrue()
            assertThat(secondResult).isFalse()
            coVerify(exactly = 2) { fragmentRepository.softDelete(fragmentId) }
        }
    }

    @Nested
    @DisplayName("Design Principle Verification")
    inner class DesignPrinciples {

        @Test
        fun `should delegate to repository softDelete, not physical delete`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()
            coEvery { fragmentRepository.softDelete(fragmentId) } returns true

            // When
            useCase.execute(fragmentId)

            // Then - verify soft delete is called, not any physical delete
            coVerify(exactly = 1) { fragmentRepository.softDelete(fragmentId) }
        }

        @Test
        fun `should preserve append-only principle by using soft delete`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()
            coEvery { fragmentRepository.softDelete(fragmentId) } returns true

            // When
            val result = useCase.execute(fragmentId)

            // Then
            // The use case only calls softDelete, preserving the append-only principle
            // The actual data is never physically removed
            assertThat(result).isTrue()
            coVerify { fragmentRepository.softDelete(fragmentId) }
        }
    }
}

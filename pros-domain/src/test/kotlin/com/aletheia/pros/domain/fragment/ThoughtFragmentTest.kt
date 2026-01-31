package com.aletheia.pros.domain.fragment

import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("ThoughtFragment Domain Tests")
class ThoughtFragmentTest {

    private val userId = UserId.generate()
    private val embedding = Embedding(FloatArray(1536) { 0.1f })

    @Nested
    @DisplayName("Creation")
    inner class CreationTests {

        @Test
        fun `should create fragment with valid input`() {
            val fragment = ThoughtFragment.create(
                userId = userId,
                textRaw = "오늘 정말 행복한 하루였다",
                moodValence = MoodValence(0.8),
                arousal = Arousal(0.6)
            )

            assertThat(fragment.id).isNotNull
            assertThat(fragment.userId).isEqualTo(userId)
            assertThat(fragment.textRaw).isEqualTo("오늘 정말 행복한 하루였다")
            assertThat(fragment.moodValence.value).isEqualTo(0.8)
            assertThat(fragment.arousal.value).isEqualTo(0.6)
            assertThat(fragment.deletedAt).isNull()
            assertThat(fragment.isDeleted).isFalse()
        }

        @Test
        fun `should generate unique IDs for each fragment`() {
            val fragment1 = createFragment()
            val fragment2 = createFragment()

            assertThat(fragment1.id).isNotEqualTo(fragment2.id)
        }

        @Test
        fun `should set createdAt to current time`() {
            val before = Instant.now()
            val fragment = createFragment()
            val after = Instant.now()

            assertThat(fragment.createdAt).isBetween(before, after)
        }

        @Test
        fun `should reject empty text`() {
            assertThatThrownBy {
                ThoughtFragment.create(
                    userId = userId,
                    textRaw = "",
                    moodValence = MoodValence(0.5),
                    arousal = Arousal(0.5)
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("text")
        }

        @Test
        fun `should reject blank text`() {
            assertThatThrownBy {
                ThoughtFragment.create(
                    userId = userId,
                    textRaw = "   ",
                    moodValence = MoodValence(0.5),
                    arousal = Arousal(0.5)
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("MoodValence Validation")
    inner class MoodValenceTests {

        @Test
        fun `should accept valence at lower bound -1`() {
            val fragment = createFragment(moodValence = MoodValence(-1.0))
            assertThat(fragment.moodValence.value).isEqualTo(-1.0)
        }

        @Test
        fun `should accept valence at upper bound 1`() {
            val fragment = createFragment(moodValence = MoodValence(1.0))
            assertThat(fragment.moodValence.value).isEqualTo(1.0)
        }

        @Test
        fun `should accept neutral valence 0`() {
            val fragment = createFragment(moodValence = MoodValence(0.0))
            assertThat(fragment.moodValence.value).isEqualTo(0.0)
            assertThat(fragment.moodValence.isNeutral).isTrue()
        }

        @Test
        fun `should identify positive valence`() {
            val fragment = createFragment(moodValence = MoodValence(0.5))
            assertThat(fragment.moodValence.isPositive).isTrue()
            assertThat(fragment.moodValence.isNegative).isFalse()
        }

        @Test
        fun `should identify negative valence`() {
            val fragment = createFragment(moodValence = MoodValence(-0.5))
            assertThat(fragment.moodValence.isNegative).isTrue()
            assertThat(fragment.moodValence.isPositive).isFalse()
        }

        @Test
        fun `should reject valence below -1`() {
            assertThatThrownBy {
                MoodValence(-1.1)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should reject valence above 1`() {
            assertThatThrownBy {
                MoodValence(1.1)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("Arousal Validation")
    inner class ArousalTests {

        @Test
        fun `should accept arousal at lower bound 0`() {
            val fragment = createFragment(arousal = Arousal(0.0))
            assertThat(fragment.arousal.value).isEqualTo(0.0)
        }

        @Test
        fun `should accept arousal at upper bound 1`() {
            val fragment = createFragment(arousal = Arousal(1.0))
            assertThat(fragment.arousal.value).isEqualTo(1.0)
        }

        @Test
        fun `should identify high arousal`() {
            val fragment = createFragment(arousal = Arousal(0.8))
            assertThat(fragment.arousal.isHigh).isTrue()
            assertThat(fragment.arousal.isLow).isFalse()
        }

        @Test
        fun `should identify low arousal`() {
            val fragment = createFragment(arousal = Arousal(0.2))
            assertThat(fragment.arousal.isLow).isTrue()
            assertThat(fragment.arousal.isHigh).isFalse()
        }

        @Test
        fun `should reject negative arousal`() {
            assertThatThrownBy {
                Arousal(-0.1)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should reject arousal above 1`() {
            assertThatThrownBy {
                Arousal(1.1)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("Soft Delete")
    inner class SoftDeleteTests {

        @Test
        fun `should mark fragment as deleted`() {
            val fragment = createFragment()

            val deletedFragment = fragment.softDelete()

            assertThat(deletedFragment.isDeleted).isTrue()
            assertThat(deletedFragment.deletedAt).isNotNull()
        }

        @Test
        fun `should preserve original data after soft delete`() {
            val fragment = createFragment(text = "Important thought")

            val deletedFragment = fragment.softDelete()

            assertThat(deletedFragment.textRaw).isEqualTo("Important thought")
            assertThat(deletedFragment.id).isEqualTo(fragment.id)
        }

        @Test
        fun `should reject second delete attempt`() {
            val fragment = createFragment()
            val deleted = fragment.softDelete()

            assertThatThrownBy {
                deleted.softDelete()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("already deleted")
        }
    }

    @Nested
    @DisplayName("Embedding")
    inner class EmbeddingTests {

        @Test
        fun `should add embedding to fragment`() {
            val fragment = createFragment()

            val withEmb = fragment.withEmbedding(embedding)

            assertThat(withEmb.hasEmbedding).isTrue()
            assertThat(withEmb.embedding?.dimension).isEqualTo(1536)
        }

        @Test
        fun `should reject adding embedding twice`() {
            val fragment = createFragment()
            val withEmb = fragment.withEmbedding(embedding)

            assertThatThrownBy {
                withEmb.withEmbedding(embedding)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("already has an embedding")
        }

        @Test
        fun `should calculate cosine similarity`() {
            val emb1 = Embedding(floatArrayOf(1f, 0f, 0f))
            val emb2 = Embedding(floatArrayOf(1f, 0f, 0f))
            val emb3 = Embedding(floatArrayOf(0f, 1f, 0f))

            assertThat(emb1.cosineSimilarity(emb2)).isEqualTo(1.0, org.assertj.core.data.Offset.offset(0.001))
            assertThat(emb1.cosineSimilarity(emb3)).isEqualTo(0.0, org.assertj.core.data.Offset.offset(0.001))
        }
    }

    // Helper method
    private fun createFragment(
        text: String = "Test thought",
        moodValence: MoodValence = MoodValence(0.5),
        arousal: Arousal = Arousal(0.5)
    ): ThoughtFragment {
        return ThoughtFragment.create(
            userId = userId,
            textRaw = text,
            moodValence = moodValence,
            arousal = arousal
        )
    }
}

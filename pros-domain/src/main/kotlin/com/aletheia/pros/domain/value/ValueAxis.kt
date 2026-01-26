package com.aletheia.pros.domain.value

/**
 * ValueAxis represents the 8 fixed dimensions of human values.
 *
 * These axes are IMMUTABLE and form the foundation of the Value Graph.
 * Each thought fragment can be mapped to multiple axes with varying weights.
 *
 * Design Decision: We use exactly 8 axes to balance:
 * - Comprehensiveness: Cover major life domains
 * - Simplicity: Easy to understand and reason about
 * - Distinctiveness: Minimal overlap between axes
 */
enum class ValueAxis(
    val displayNameKo: String,
    val displayNameEn: String,
    val description: String
) {
    /**
     * Growth/Learning - 성장/학습
     * The drive to learn, improve, and develop new skills.
     */
    GROWTH(
        displayNameKo = "성장/학습",
        displayNameEn = "Growth/Learning",
        description = "The drive to learn, improve, and develop new skills or knowledge"
    ),

    /**
     * Stability/Predictability - 안정/예측가능
     * The need for security, routine, and predictable outcomes.
     */
    STABILITY(
        displayNameKo = "안정/예측가능",
        displayNameEn = "Stability/Predictability",
        description = "The need for security, routine, and predictable outcomes"
    ),

    /**
     * Financial/Reward - 금전/보상
     * Concerns about money, compensation, and material rewards.
     */
    FINANCIAL(
        displayNameKo = "금전/보상",
        displayNameEn = "Financial/Reward",
        description = "Concerns about money, compensation, and material rewards"
    ),

    /**
     * Autonomy/Control - 자율/통제
     * The desire for independence, self-direction, and control over one's life.
     */
    AUTONOMY(
        displayNameKo = "자율/통제",
        displayNameEn = "Autonomy/Control",
        description = "The desire for independence, self-direction, and control over one's life"
    ),

    /**
     * Relationship/Belonging - 관계/소속
     * The need for social connection, belonging, and meaningful relationships.
     */
    RELATIONSHIP(
        displayNameKo = "관계/소속",
        displayNameEn = "Relationship/Belonging",
        description = "The need for social connection, belonging, and meaningful relationships"
    ),

    /**
     * Achievement/Recognition - 성취/인정
     * The drive for accomplishment, status, and recognition from others.
     */
    ACHIEVEMENT(
        displayNameKo = "성취/인정",
        displayNameEn = "Achievement/Recognition",
        description = "The drive for accomplishment, status, and recognition from others"
    ),

    /**
     * Health/Energy - 건강/에너지
     * Concerns about physical and mental wellbeing, vitality, and energy levels.
     */
    HEALTH(
        displayNameKo = "건강/에너지",
        displayNameEn = "Health/Energy",
        description = "Concerns about physical and mental wellbeing, vitality, and energy levels"
    ),

    /**
     * Meaning/Contribution - 의미/기여
     * The search for purpose, meaning, and contribution to something larger than oneself.
     */
    MEANING(
        displayNameKo = "의미/기여",
        displayNameEn = "Meaning/Contribution",
        description = "The search for purpose, meaning, and contribution to something larger than oneself"
    );

    companion object {
        /**
         * Returns all value axes in order.
         */
        fun all(): List<ValueAxis> = entries

        /**
         * Number of value axes (always 8).
         */
        const val COUNT = 8

        /**
         * Finds a value axis by name (case-insensitive).
         */
        fun fromName(name: String): ValueAxis? =
            entries.find { it.name.equals(name, ignoreCase = true) }
    }
}

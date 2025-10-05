package com.sovereign_rise.app.domain.model

enum class OnboardingScreen {
    WELCOME,
    PROGRESSION,
    TRUTH,
    SOCIAL,
    BEGIN
}

enum class TutorialDay {
    DAY_1,
    DAY_2,
    DAY_3,
    DAY_7,
    COMPLETED
}

data class TutorialTask(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean
)

data class TutorialState(
    val currentDay: TutorialDay,
    val completedTasks: List<String>,
    val unlockedFeatures: List<String>,
    val maxTasksAllowed: Int,
    val isCompleted: Boolean
)



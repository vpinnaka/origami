package com.origami.assistant.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object ModelSetup : Screen("model_setup")
    object Chat : Screen("chat?conversationId={conversationId}") {
        fun withId(id: String = "") = if (id.isBlank()) "chat" else "chat?conversationId=$id"
    }
    object Assistants : Screen("assistants")
    object AssistantDetail : Screen("assistant/{assistantId}") {
        fun withId(id: String) = "assistant/$id"
    }
    object Skills : Screen("skills")
    object Settings : Screen("settings")
}

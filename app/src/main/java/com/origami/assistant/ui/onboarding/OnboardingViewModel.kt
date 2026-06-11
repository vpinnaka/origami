package com.origami.assistant.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.origami.assistant.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val step: Int = 0,
    val userName: String = "",
    val selectedProfile: String = "",
    val selectedSkills: Set<String> = emptySet(),
    val isCompleting: Boolean = false
)

val PROFILES = listOf(
    "Developer" to "Helps with coding, debugging, technical Q&A",
    "Writer" to "Drafting, editing, research, brainstorming",
    "Student" to "Learning, summarizing, explaining concepts",
    "Professional" to "Email, scheduling, research, productivity",
    "Personal" to "Daily life, reminders, general assistant"
)

val DEFAULT_SKILLS = listOf(
    "web_search" to "Search the web for current information",
    "calendar" to "Read and create calendar events",
    "calculate" to "Perform calculations",
    "execute_code" to "Run Python or shell scripts"
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onNameChange(name: String) = _state.update { it.copy(userName = name) }

    fun onProfileSelect(profile: String) = _state.update { it.copy(selectedProfile = profile) }

    fun onSkillToggle(skill: String) = _state.update { state ->
        val skills = state.selectedSkills.toMutableSet()
        if (skill in skills) skills.remove(skill) else skills.add(skill)
        state.copy(selectedSkills = skills)
    }

    fun nextStep() = _state.update { it.copy(step = it.step + 1) }
    fun prevStep() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isCompleting = true) }
            val s = _state.value
            if (s.userName.isNotBlank()) prefs.setUserName(s.userName)
            prefs.setOnboardingDone(true)
            onDone()
        }
    }
}

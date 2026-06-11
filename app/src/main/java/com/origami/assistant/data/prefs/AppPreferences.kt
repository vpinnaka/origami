package com.origami.assistant.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("origami_prefs")

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val store = context.dataStore

    companion object {
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_MODEL_PATH = stringPreferencesKey("model_path")
        val KEY_INFERENCE_BACKEND = stringPreferencesKey("inference_backend")  // "cpu" | "gpu"
        val KEY_TAVILY_API_KEY = stringPreferencesKey("tavily_api_key")
        val KEY_COMPOSIO_API_KEY = stringPreferencesKey("composio_api_key")
        val KEY_ACTIVE_CONVERSATION = stringPreferencesKey("active_conversation_id")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_PROFILE = stringPreferencesKey("user_profile")  // JSON blob
        val KEY_MAX_CONTEXT_TOKENS = intPreferencesKey("max_context_tokens")
        val KEY_MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val KEY_MODEL_DOWNLOADED = booleanPreferencesKey("model_downloaded")
    }

    val onboardingDone: Flow<Boolean> = store.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val modelPath: Flow<String> = store.data.map { it[KEY_MODEL_PATH] ?: "" }
    val inferenceBackend: Flow<String> = store.data.map { it[KEY_INFERENCE_BACKEND] ?: "cpu" }
    val tavilyApiKey: Flow<String> = store.data.map { it[KEY_TAVILY_API_KEY] ?: "" }
    val composioApiKey: Flow<String> = store.data.map { it[KEY_COMPOSIO_API_KEY] ?: "" }
    val activeConversationId: Flow<String> = store.data.map { it[KEY_ACTIVE_CONVERSATION] ?: "" }
    val userName: Flow<String> = store.data.map { it[KEY_USER_NAME] ?: "" }
    val maxContextTokens: Flow<Int> = store.data.map { it[KEY_MAX_CONTEXT_TOKENS] ?: 4096 }
    val memoryEnabled: Flow<Boolean> = store.data.map { it[KEY_MEMORY_ENABLED] ?: true }
    val modelDownloaded: Flow<Boolean> = store.data.map { it[KEY_MODEL_DOWNLOADED] ?: false }

    suspend fun setOnboardingDone(done: Boolean) =
        store.edit { it[KEY_ONBOARDING_DONE] = done }

    suspend fun setModelPath(path: String) =
        store.edit { it[KEY_MODEL_PATH] = path }

    suspend fun setInferenceBackend(backend: String) =
        store.edit { it[KEY_INFERENCE_BACKEND] = backend }

    suspend fun setTavilyApiKey(key: String) =
        store.edit { it[KEY_TAVILY_API_KEY] = key }

    suspend fun setComposioApiKey(key: String) =
        store.edit { it[KEY_COMPOSIO_API_KEY] = key }

    suspend fun setActiveConversation(id: String) =
        store.edit { it[KEY_ACTIVE_CONVERSATION] = id }

    suspend fun setUserName(name: String) =
        store.edit { it[KEY_USER_NAME] = name }

    suspend fun setMaxContextTokens(tokens: Int) =
        store.edit { it[KEY_MAX_CONTEXT_TOKENS] = tokens }

    suspend fun setMemoryEnabled(enabled: Boolean) =
        store.edit { it[KEY_MEMORY_ENABLED] = enabled }

    suspend fun setModelDownloaded(downloaded: Boolean) =
        store.edit { it[KEY_MODEL_DOWNLOADED] = downloaded }
}

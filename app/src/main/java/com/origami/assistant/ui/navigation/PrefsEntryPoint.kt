package com.origami.assistant.ui.navigation

import com.origami.assistant.data.prefs.AppPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PrefsEntryPoint {
    fun prefs(): AppPreferences
}

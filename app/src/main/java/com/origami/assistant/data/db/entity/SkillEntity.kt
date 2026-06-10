package com.origami.assistant.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val version: String = "1.0.0",
    /** Absolute path to the skill folder on device */
    val folderPath: String,
    /** Contents of SKILL.md */
    val manifest: String,
    /** Comma-separated list of script entry-points */
    val entryPoints: String = "run.sh",
    val isEnabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis()
)

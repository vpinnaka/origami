package com.origami.assistant.skills

import android.content.Context
import com.origami.assistant.data.db.entity.SkillEntity
import com.origami.assistant.data.repository.AssistantRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Agent Skills — the ecosystem of SKILL.md + script bundles.
 *
 * Skill folder structure:
 *   skill-name/
 *     SKILL.md          (manifest: name, description, entry points)
 *     run.sh            (main entry point)
 *     *.py / *.sh       (supporting scripts)
 *
 * Skills live in app's private files/skills/ directory.
 */
@Singleton
class SkillManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assistantRepo: AssistantRepository
) {
    val skillsDir: File = File(context.filesDir, "skills").also { it.mkdirs() }

    /** Install a skill from a source folder (copy + register) */
    suspend fun installFromFolder(sourceFolder: File): Result<SkillEntity> {
        val manifest = File(sourceFolder, "SKILL.md")
        if (!manifest.exists()) {
            return Result.failure(IllegalArgumentException("No SKILL.md found in ${sourceFolder.name}"))
        }
        return try {

        val manifestContent = manifest.readText()
        val name = parseManifestField(manifestContent, "name") ?: sourceFolder.name
        val description = parseManifestField(manifestContent, "description") ?: ""
        val entryPoints = parseManifestField(manifestContent, "entry_points") ?: "run.sh"
        val version = parseManifestField(manifestContent, "version") ?: "1.0.0"

        val skillId = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val destFolder = File(skillsDir, skillId).also { it.mkdirs() }

        // Copy all files
        sourceFolder.listFiles()?.forEach { file ->
            file.copyTo(File(destFolder, file.name), overwrite = true)
            if (file.name.endsWith(".sh") || file.name.endsWith(".py")) {
                File(destFolder, file.name).setExecutable(true)
            }
        }

        val skill = SkillEntity(
            id = skillId,
            name = name,
            description = description,
            version = version,
            folderPath = destFolder.absolutePath,
            manifest = manifestContent,
            entryPoints = entryPoints
        )
        assistantRepo.saveSkill(skill)
        Timber.i("Installed skill: $name v$version")
        Result.success(skill)
        } catch (e: Exception) {
            Timber.e(e, "Failed to install skill")
            Result.failure(e)
        }
    }

    /** Install a bundled (built-in) skill from assets */
    suspend fun installBuiltIn(assetPath: String): Result<SkillEntity> = try {
        val tempDir = File(context.cacheDir, "skill_install_${UUID.randomUUID()}").also { it.mkdirs() }
        context.assets.list(assetPath)?.forEach { fileName ->
            val content = context.assets.open("$assetPath/$fileName").readBytes()
            File(tempDir, fileName).writeBytes(content)
        }
        installFromFolder(tempDir).also { tempDir.deleteRecursively() }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun uninstall(skillId: String) {
        val skill = assistantRepo.getSkill(skillId) ?: return
        File(skill.folderPath).deleteRecursively()
        assistantRepo.deleteSkill(skillId)
    }

    private fun parseManifestField(content: String, field: String): String? {
        val pattern = Regex("^${field}:\\s*(.+)$", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
        return pattern.find(content)?.groupValues?.get(1)?.trim()
    }
}

import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application)     apply false
    alias(libs.plugins.compose.compiler)        apply false
    alias(libs.plugins.kotlin.serialization)    apply false
    alias(libs.plugins.ksp)                     apply false
    alias(libs.plugins.room)                    apply false
}

// Optional BUILD_DIR in local.properties moves build output out of the project
// folder. Needed when the project sits in a synced folder (e.g. OneDrive) that
// won't let Gradle delete and rewrite its build files. Unset = the usual app/build.
val buildDirOverride: String? = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}.getProperty("BUILD_DIR")?.trim()?.takeIf { it.isNotEmpty() }

if (buildDirOverride != null) {
    allprojects {
        layout.buildDirectory.set(file("$buildDirOverride/${project.name}"))
    }
}

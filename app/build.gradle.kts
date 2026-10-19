plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace   = "com.adheesh.gid"
    compileSdk  = 36

    defaultConfig {
        applicationId = "com.adheesh.gid"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0"

        // Inject Supabase keys from local.properties into BuildConfig.
        // NEVER put actual values here – they are read at build time only.
        val props = rootProject.file("local.properties")
            .takeIf { it.exists() }
            ?.let { java.util.Properties().apply { load(it.inputStream()) } }
        buildConfigField("String", "SUPABASE_URL",           "\"${props?.getProperty("SUPABASE_URL", "") ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY",      "\"${props?.getProperty("SUPABASE_ANON_KEY", "") ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID",   "\"${props?.getProperty("GOOGLE_OAUTH_WEB_CLIENT_ID", "") ?: ""}\"")
    }

    buildTypes {
        debug {
            // BuildConfig.DEBUG = true in debug builds, enabling the seed screen
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Enable java.time desugaring for minSdk 26 (needed for LocalDate)
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose     = true
        buildConfig = true   // gives us BuildConfig.DEBUG for the seed screen
        aidl        = false
        shaders     = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room schema output directory – check schemas into git for migration history
room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // java.time desugaring – required when minSdk < 26 uses java.time;
    // we keep it even at minSdk 26 to avoid subtle desugaring issues on some OEMs
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Lifecycle / ViewModel
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation 3
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Supabase – BOM manages all module versions
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)

    // Ktor – HTTP engine required by supabase-kt on Android
    implementation(libs.ktor.client.android)

    // Credential Manager + Google ID (Step D – Google Sign-In)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

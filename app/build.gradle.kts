import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.misw4203.vinilos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.misw4203.vinilos"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.misw4203.vinilos.HiltTestRunner"
    }

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
    }
    fun keystoreValue(propKey: String, envKey: String): String? =
        keystoreProps.getProperty(propKey) ?: System.getenv(envKey)

    signingConfigs {
        create("release") {
            val storePw = keystoreValue("storePassword", "VINILOS_STORE_PASSWORD")
            val keyPw = keystoreValue("keyPassword", "VINILOS_KEY_PASSWORD")
            val alias = keystoreValue("keyAlias", "VINILOS_KEY_ALIAS")
            val storePath = keystoreValue("storeFile", "VINILOS_STORE_FILE")
                ?: "vinilos-release.jks"
            if (storePw != null && keyPw != null && alias != null) {
                storeFile = file(storePath)
                storePassword = storePw
                keyAlias = alias
                keyPassword = keyPw
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/\"")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"http://backvynils.duckdns.org:3000/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle / ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.text.google.fonts)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coil
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// --- Monkey: pruebas de exploración aleatoria sistemática ---
// Lanza la matriz de semillas vía scripts/monkey.{ps1,sh}. Requiere un
// emulador/dispositivo arrancado (ver flujo híbrido E2E en CLAUDE.md).
// Configurable: -PmonkeyEvents=, -PmonkeySeeds=, -PmonkeyThrottle=.
tasks.register<Exec>("monkeyTest") {
    group = "verification"
    description = "Random systematic exploration with the Android Monkey (seed matrix)."
    workingDir = rootProject.projectDir
    val scriptsDir = rootProject.file("scripts")
    val events = (project.findProperty("monkeyEvents") as String?) ?: "500"
    val seeds = (project.findProperty("monkeySeeds") as String?) ?: "1 42 123 2024 7777"
    val throttle = (project.findProperty("monkeyThrottle") as String?) ?: "200"
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    if (isWindows) {
        commandLine(
            "powershell", "-ExecutionPolicy", "Bypass", "-File",
            scriptsDir.resolve("monkey.ps1").absolutePath,
            "-Events", events, "-Seeds", seeds, "-Throttle", throttle,
        )
    } else {
        commandLine(
            "bash", scriptsDir.resolve("monkey.sh").absolutePath,
            "--events", events, "--seeds", seeds, "--throttle", throttle,
        )
    }
}
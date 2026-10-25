plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/** Commits on HEAD; needs full history, so CI checks out with fetch-depth 0. */
val commitCount: Int = providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
    .standardOutput.asText.get().trim().toInt()

android {
    namespace = "com.tritv.photo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tritv.photo"
        minSdk = 23
        targetSdk = 35
        // One more per commit, so each new build installs as an upgrade; TV launchers such as
        // Sony's cache the banner per version and kept showing the old one otherwise.
        versionCode = commitCount
        versionName = "0.1.$commitCount"
    }

    // Shared debug key (the same one tri.iptv uses) so every CI build installs over the previous
    // one and keeps the saved album. Only fit for sideloading.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        // The build to put on a TV: not debuggable, so ART compiles it fully, and shrunk and
        // optimised by R8, which Compose relies on for its speed.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.savedstate)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.tv.material)

    // One OkHttp client carries the album's session cookie for the API, the images and the videos.
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.ui)

    implementation(libs.kotlinx.coroutines.android)

    // Installs the baseline profiles (Compose's own and src/main/baseline-prof.txt) on sideloaded
    // installs, so start-up and the slideshow run precompiled instead of interpreted.
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}

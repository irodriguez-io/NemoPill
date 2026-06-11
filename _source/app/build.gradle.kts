plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

android {
    namespace = "io.nemopill.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.nemopill.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Security: ADR-021 — no cloud backup, no device transfer.
    // See app/src/main/res/xml/data_extraction_rules.xml
}

// Room schema export (file 06 § Section 2). The KSP-based Room processor writes the committed
// schema JSON to app/schemas/io.nemopill.app.NemoPillDatabase/<version>.json on every :app
// compile; 1.json is checked in with T-010. Required because NemoPillDatabase sets
// exportSchema = true (a missing location would fail the build / silently skip export).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":medication-management"))
    implementation(project(":scheduling"))
    implementation(project(":notifications"))
    implementation(project(":adherence-tracking"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.vm.ktx)
    implementation(libs.androidx.activity.compose)

    // DI graph (T-008): Hilt runtime + KSP-driven codegen lives here (first @Module + entry points).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room (T-010): the single NemoPillDatabase @Database lives here. KSP-driven Room codegen
    // (the DAO impls aggregated from the feature modules' @Dao interfaces) runs in :app — the
    // module that owns the @Database (file 04 single-instance design; T-010 ADR minimal KSP
    // placement). room-ktx supplies the withTransaction extension used by RoomConfirmationRepository.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines — viewModelScope drives the suspend use case off the demo screen.
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    // Roborazzi snapshot infrastructure (AC-004 / T-007).
    // BOM applied on test classpath to align Compose transitive deps from roborazzi-compose.
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)

    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

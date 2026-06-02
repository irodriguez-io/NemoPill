plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

android {
    namespace = "io.nemopill.adherencetracking"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    annotationProcessor(libs.androidx.room.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.junit.ext)
}

// ---------------------------------------------------------------------------
// Kover coverage thresholds — :adherence-tracking (ADR-044 / T-007)
// domain ≥ 90 %, application ≥ 80 %. No threshold on infrastructure/presentation.
// Vacuously satisfied at T-007 because src/main/kotlin is empty.
// ---------------------------------------------------------------------------
koverReport {
    verify {
        rule("adherence-tracking domain line coverage ≥ 90 %") {
            filters {
                includes {
                    packages("io.nemopill.adherencetracking.domain")
                }
            }
            bound { minValue = 90 }
        }
        rule("adherence-tracking application line coverage ≥ 80 %") {
            filters {
                includes {
                    packages("io.nemopill.adherencetracking.application")
                }
            }
            bound { minValue = 80 }
        }
    }
}

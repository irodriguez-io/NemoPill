plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

android {
    namespace = "io.nemopill.scheduling"
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
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.junit.ext)
}

// ---------------------------------------------------------------------------
// Kover coverage thresholds — :scheduling (ADR-044 / T-007)
// domain ≥ 90 %, application ≥ 80 %. No threshold on infrastructure/presentation.
// Vacuously satisfied at T-007 because src/main/kotlin is empty.
// ---------------------------------------------------------------------------
koverReport {
    verify {
        rule("scheduling domain line coverage ≥ 90 %") {
            filters {
                includes {
                    packages("io.nemopill.scheduling.domain")
                }
            }
            bound { minValue = 90 }
        }
        rule("scheduling application line coverage ≥ 80 %") {
            filters {
                includes {
                    packages("io.nemopill.scheduling.application")
                }
            }
            bound { minValue = 80 }
        }
    }
}

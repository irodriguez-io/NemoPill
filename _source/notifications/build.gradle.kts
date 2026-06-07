plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

android {
    namespace = "io.nemopill.notifications"
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

    // androidx.core.ktx supplies NotificationManagerCompat / NotificationCompat /
    // NotificationChannelCompat — the whole notification surface this module renders.
    implementation(libs.androidx.core.ktx)

    // Coroutines — the ReminderFiredListener collects the :core DomainEventPublisher Flow and the
    // NotificationPort is a suspend boundary.
    implementation(libs.kotlinx.coroutines.core)

    // Hilt library only (no Hilt Gradle plugin / KSP here): the adapter, builder, factory, and
    // use case are @Inject-constructor types aggregated into :app's Hilt graph — mirroring the
    // T-008 :scheduling minimal-placement pattern. ConfirmFromNotificationReceiver is a plain
    // (non-injected) stub in T-009, so no @AndroidEntryPoint codegen is needed in this module.
    implementation(libs.hilt.android)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit.ext)
}

// Kover per-package coverage gate (ADR-086 report-level-filter form). Kover 0.8.x forbids
// per-rule filters, so the report-wide filter scopes both the report and the verify gate to the
// Application layer: io.nemopill.notifications.application.* >= 80% line (PresentReminderUseCase,
// ReminderFiredListener). domain.* is vacuous (none yet); infrastructure.* and the module-root
// constants carry no % threshold and are exercised by the Robolectric integration test.
kover {
    reports {
        filters {
            includes {
                classes("io.nemopill.notifications.application.*")
            }
        }
        verify {
            rule("io.nemopill.notifications.application line coverage >= 80%") {
                bound {
                    minValue = 80
                }
            }
        }
    }
}

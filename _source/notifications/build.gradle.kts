plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
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
    // NotificationChannelCompat — the whole notification surface this module renders — plus
    // androidx.annotation (@VisibleForTesting on the receiver's test seams).
    implementation(libs.androidx.core.ktx)

    // Coroutines — the ReminderFiredListener collects the :core DomainEventPublisher Flow, and the
    // ConfirmFromNotificationReceiver now runs a bounded goAsync() scope for the suspend confirm
    // dispatch (file 06 § Timeout rule).
    implementation(libs.kotlinx.coroutines.core)

    // Hilt + KSP (T-010): ConfirmFromNotificationReceiver now declares a Hilt @EntryPoint to obtain
    // the :core NotificationConfirmationGateway via EntryPointAccessors (a manifest receiver is
    // OS-instantiated and cannot be constructor-injected). Declaring the @EntryPoint requires Hilt
    // codegen in this module — refining the T-009 minimal-placement choice exactly as ADR-091 did
    // for :scheduling's ReminderAlarmReceiver. The @Inject-constructor adapters remain aggregated
    // into :app's Hilt graph.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    // T-010 end-to-end receiver test (AC-004): drives the action Intent through the receiver to a
    // real Room Confirmation write. The seam port lives in :core, but the real persistence path
    // (RoomConfirmationRepository + ConfirmDoseFromReminderUseCase + AdherenceConfirmationGateway +
    // ConfirmationEntity/Dao/Converters) lives in :adherence-tracking — a TEST-ONLY dependency (the
    // production graph stays clean; Konsist excludes /test/ paths). The in-memory test database
    // declared in this test source set needs Room codegen, hence kspTest(room.compiler).
    testImplementation(project(":adherence-tracking"))
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.room.ktx)
    kspTest(libs.androidx.room.compiler)

    androidTestImplementation(libs.junit.ext)
}

// Kover per-package coverage gate (ADR-086 report-level-filter form). Kover 0.8.x forbids per-rule
// filters, so the report-wide filter scopes both the report and the verify gate to the Application
// layer: io.nemopill.notifications.application.* >= 80% line (PresentReminderUseCase,
// ReminderFiredListener). domain.* is vacuous; infrastructure.* and the module-root constants carry
// no % threshold and are exercised by the Robolectric integration tests.
//
// T-010 added the Hilt Gradle plugin + KSP to this module (for the receiver @EntryPoint), so Dagger
// now generates *_Factory classes (e.g. PresentReminderUseCase_Factory) IN this module's
// application package. Generated factories must never count toward coverage — exclude them by
// Dagger's generation annotation (the same ADR-091 fix applied to :scheduling). Still a
// report-level filter (not a per-rule filter, which Kover 0.8.x forbids).
kover {
    reports {
        filters {
            includes {
                classes("io.nemopill.notifications.application.*")
            }
            excludes {
                annotatedBy("dagger.internal.DaggerGenerated")
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

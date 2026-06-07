plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
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

    // Coroutines — the ReminderAlarmReceiver publishes ReminderFired on a bounded goAsync()
    // scope (file 06 § Timeout rule).
    implementation(libs.kotlinx.coroutines.android)

    // Hilt + KSP (T-009): ReminderAlarmReceiver is an @AndroidEntryPoint broadcast receiver. A
    // manifest receiver is OS-instantiated and cannot be constructor-injected, so member
    // injection requires Hilt codegen (the generated Hilt_ReminderAlarmReceiver base) in this
    // module. This refines the T-008 ADR-083 minimal-Hilt-placement choice for the receiver case;
    // the @Inject-constructor adapters are still aggregated into :app's Hilt graph.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit.ext)
}

// Kover per-package coverage gate (ADR-078 deferral resolved at T-008). Kover 0.8.x forbids
// per-rule filters, so the report-wide filter scopes both the report and the verify gate to
// the Application layer only: io.nemopill.scheduling.application.* >= 80% line. As a
// consequence, infrastructure.* and presentation.* are excluded from this module's coverage
// report entirely (not just un-gated) — they carry no % threshold and are exercised by the
// Robolectric integration test. Recorded as a T-008 ADR.
kover {
    reports {
        filters {
            includes {
                classes("io.nemopill.scheduling.application.*")
            }
            excludes {
                // T-009 added KSP+Hilt to :scheduling for the ReminderAlarmReceiver EntryPoint, so
                // Dagger now generates ScheduleDemoReminderUseCase_Factory IN this module's
                // application package (T-008 generated it in :app). Generated factories must never
                // count toward coverage — exclude them by Dagger's generation annotation. Still a
                // report-level filter (not a per-rule filter, which Kover 0.8.x forbids).
                annotatedBy("dagger.internal.DaggerGenerated")
            }
        }
        verify {
            rule("io.nemopill.scheduling.application line coverage >= 80%") {
                bound {
                    minValue = 80
                }
            }
        }
    }
}

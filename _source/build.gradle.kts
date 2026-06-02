import org.gradle.api.tasks.Copy

plugins {
    // The `base` plugin provides the root project's lifecycle tasks
    // (build / clean / assemble / check). Without it the root has no
    // `build` task, so `tasks.named("build")` below fails at configuration
    // time — which broke `1 · Setup` (`./gradlew help --configuration-cache`).
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ktlint) apply false
    // Kover applied to root (not `apply false`) so the `kover()` dependency
    // configuration DSL accessor is available in the root dependencies block below.
    // Submodules still apply it independently via their own plugins {} blocks.
    alias(libs.plugins.kover)
}

// ---------------------------------------------------------------------------
// Git hooks — install pre-commit hook on every build so new team members
// pick it up automatically after cloning. Hook lives in _build/hooks/.
// The task is wired to the root project's build lifecycle via dependsOn.
// ---------------------------------------------------------------------------
val hooksDir = rootDir.resolve("../_build/hooks")
val gitHooksDir = rootDir.resolve("../.git/hooks")

val installGitHooks by tasks.registering(Copy::class) {
    description = "Copies _build/hooks/* into .git/hooks/ and marks them executable."
    group = "git"

    from(hooksDir) {
        include("pre-commit")
    }
    into(gitHooksDir)

    doLast {
        fileTree(gitHooksDir) {
            include("pre-commit")
        }.forEach { hook ->
            hook.setExecutable(true)
        }
        logger.lifecycle("Git hook installed: ${gitHooksDir}/pre-commit")
    }
}

// Wire installGitHooks to run automatically on every build of the root project.
// Subproject builds do NOT re-run this task (it is root-only).
tasks.named("build") {
    dependsOn(installGitHooks)
}

// ---------------------------------------------------------------------------
// Kover multi-module aggregation (ADR-044 / T-007)
// Declare every submodule as a kover() dependency so `./gradlew koverHtmlReport`
// and `./gradlew koverVerify` aggregate coverage across the full project.
// ---------------------------------------------------------------------------
dependencies {
    kover(project(":core"))
    kover(project(":medication-management"))
    kover(project(":scheduling"))
    kover(project(":notifications"))
    kover(project(":adherence-tracking"))
    kover(project(":app"))
}

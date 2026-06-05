package io.nemopill.core.result

/**
 * Two-tier result type (file 04 § Error handling).
 *
 * [Ok] carries a success value. [Err] is the sealed error tier. The built-in
 * [Err.Unexpected] variant represents the unexpected-failure tier — an exception
 * caught at the Application/Infrastructure boundary and translated for the UI; its
 * [Err.Unexpected.message] must never carry Patient data (ADR-031). Later milestones
 * extend the [Err] catalog with expected-failure variants; they do not redesign this
 * shape.
 */
sealed interface Result<out T, out E> {
    data class Ok<out T>(val value: T) : Result<T, Nothing>

    sealed interface Err : Result<Nothing, Err> {
        /** Catch-all unexpected-failure tier. [message] is a static, non-PII string (ADR-031). */
        data class Unexpected(val message: String) : Err
    }
}

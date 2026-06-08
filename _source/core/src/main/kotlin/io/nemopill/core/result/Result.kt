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

        /**
         * Defensive parse-rejection at the notification-action entry point (file 06 § F-006;
         * file 13 "PendingIntent → notification-action receiver" boundary). Returned by
         * `ConfirmFromNotificationReceiver` when the inbound `status` extra is not a bounded
         * [io.nemopill.core.confirm.ConfirmationStatus] value (impossible from an app-generated
         * `FLAG_IMMUTABLE` PendingIntent; defensive against intent redirection). Carries **no
         * fields** — non-PII by construction, so it is exempt from ADR-049 rule (ii) (the
         * `:core` non-PII allow-list per the ADR-087 resolution). On this outcome the source
         * notification is **not** dismissed so the Patient can retry from in-app (file 06 §
         * Retry rule).
         */
        data object UnexpectedNotificationPayload : Err
    }
}

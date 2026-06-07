package io.nemopill.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.nemopill.notifications.application.ReminderFiredListener

/**
 * SingletonComponent access for the cold-start wiring in `NemoPillApplication`.
 *
 * Retrieved via `EntryPointAccessors` rather than `@Inject lateinit var` field injection: Dagger
 * 2.51.1's Kotlin-metadata reader cannot parse Kotlin 2.1.0 metadata on the members-injection
 * validation path (it reads the field-owner's metadata to check for the synthetic
 * `$annotations` property), which fails the `:app` Hilt compile. Provision entry points avoid that
 * path entirely. Recorded as a T-009 ADR; the durable fix (a Hilt version bump) is routed to a
 * follow-up rather than folded into this slice.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppStartupEntryPoint {
    fun reminderFiredListener(): ReminderFiredListener
}

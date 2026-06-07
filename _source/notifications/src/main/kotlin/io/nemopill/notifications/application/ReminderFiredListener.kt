package io.nemopill.notifications.application

import io.nemopill.core.event.DomainEventPublisher
import io.nemopill.core.event.ReminderFired
import io.nemopill.core.event.ReminderVariant
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import javax.inject.Inject

/**
 * Subscribes to the `:core` [DomainEventPublisher], filters for [ReminderFired] with
 * `variant == ON_TIME`, and invokes [PresentReminderUseCase] (file 04 § notifications::application
 * subscribers). This is the `:notifications` half of the cross-Bounded-Context seam: `:scheduling`
 * publishes, this listener consumes — neither imports the other (file 04 § Cross-module
 * communication).
 *
 * **Idempotent on `doseId`** (the dedupe key — file 06 § Idempotency rule): a replayed event for a
 * Dose already presented is skipped, so the cold-start replay buffer cannot double-render. The
 * `seen` set is touched only from the single collection coroutine.
 *
 * [start] is launched once at app startup on an application-scoped coroutine (see
 * `NemoPillApplication`) and collects for the process lifetime.
 */
class ReminderFiredListener
    @Inject
    constructor(
        private val publisher: DomainEventPublisher,
        private val presentReminder: PresentReminderUseCase,
    ) {
        private val presentedDoseIds = mutableSetOf<String>()

        suspend fun start() {
            publisher.events
                .filterIsInstance<ReminderFired>()
                .filter { it.variant == ReminderVariant.ON_TIME }
                .collect { event ->
                    if (presentedDoseIds.add(event.doseId.value)) {
                        presentReminder(event.doseId, event.variant)
                    }
                }
        }
    }

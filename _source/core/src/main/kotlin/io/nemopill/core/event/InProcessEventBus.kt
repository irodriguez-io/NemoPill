package io.nemopill.core.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The project's first event-bus production code (file 04 § DomainEventPublisher): a
 * [DomainEventPublisher] backed by a Kotlin Coroutines [MutableSharedFlow].
 *
 * **Small replay buffer for subscriber-late catch-up.** When an alarm fires after process
 * death the OS cold-starts the app, racing `NemoPillApplication.onCreate` (which registers the
 * subscriber) against the alarm receiver (which publishes). The [REPLAY] buffer lets a
 * late-registering subscriber still receive the most-recent events (file 06 § Ordering rule).
 *
 * [BufferOverflow.DROP_OLDEST] keeps [publish] non-suspending under back-pressure — a fired
 * Reminder is never blocked by a slow subscriber. Dropping is acceptable: the architecture
 * accepts no event durability for MVP (file 04 § Architecture Risks — durable delivery is the
 * Room-as-outbox path deferred to a later milestone).
 *
 * No Android types — a pure-Kotlin `:core` shared-kernel type. DI-bound as an `@Singleton`
 * (one bus per process) in `:app`.
 */
class InProcessEventBus(
    replay: Int = REPLAY,
) : DomainEventPublisher {
    private val mutableEvents =
        MutableSharedFlow<DomainEvent>(
            replay = replay,
            extraBufferCapacity = EXTRA_BUFFER,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    override val events: Flow<DomainEvent> = mutableEvents.asSharedFlow()

    override suspend fun publish(event: DomainEvent) {
        mutableEvents.emit(event)
    }

    private companion object {
        /** Replay depth for the cold-start subscriber-late race (file 06 § Ordering rule). */
        const val REPLAY = 8

        /** Headroom so [publish] stays non-suspending when a subscriber lags. */
        const val EXTRA_BUFFER = 16
    }
}

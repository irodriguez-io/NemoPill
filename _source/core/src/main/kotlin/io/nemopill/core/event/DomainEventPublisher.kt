package io.nemopill.core.event

import kotlinx.coroutines.flow.Flow

/**
 * The cross-module event seam (file 04 § DomainEventPublisher). Feature modules [publish]
 * [DomainEvent]s and subscribe to [events]; this is the only legal cross-Bounded-Context path
 * (file 04 § Cross-module communication).
 *
 * In-process only for MVP (file 02 handoff: no message bus, no Room-as-outbox). Subscribers
 * register at app startup. **Idempotency is the subscriber's responsibility** — every event
 * documents its dedupe key (file 06 § Idempotency rule) and subscribers honor it.
 *
 * The single implementation is [InProcessEventBus], DI-bound as an `@Singleton` in `:app`.
 */
interface DomainEventPublisher {
    suspend fun publish(event: DomainEvent)

    val events: Flow<DomainEvent>
}

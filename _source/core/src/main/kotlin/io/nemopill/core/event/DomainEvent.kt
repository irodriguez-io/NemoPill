package io.nemopill.core.event

/**
 * Root of the `:core` domain-event hierarchy (file 04 § shared kernel; file 02 § Domain Events).
 *
 * Cross-Bounded-Context communication is event-driven: a feature module never imports another
 * feature's Domain or Application packages — it emits a [DomainEvent] on the [DomainEventPublisher]
 * (in `:core`) and other modules subscribe (file 04 § Cross-module communication).
 *
 * T-009 seeds the hierarchy with [ReminderFired] only. The other members named in file 02
 * (`DoseMaterialized`, `DoseMissed`, `DeviceTimezoneChanged`, `MedicationCreated`,
 * `DoseScheduleReplaced`, `MedicationArchived`, `DoseConfirmed`) are additive-only and land with
 * their owning features.
 */
sealed interface DomainEvent

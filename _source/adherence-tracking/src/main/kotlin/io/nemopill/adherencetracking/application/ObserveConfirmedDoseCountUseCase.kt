package io.nemopill.adherencetracking.application

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UC — observe the on-screen "doses Taken" count (M-002 Done-When item (5), the observe leg).
 * `:adherence-tracking`'s **first query use case** and the project's first read/observe path.
 *
 * A thin Application pass-through over [ConfirmationRepository.observeConfirmedDoseCount]: it exists
 * so Presentation depends on the Application port surface, never on the Infrastructure adapter
 * (file 04 § layer table — Presentation → Application use cases). The demo counter is a **raw tally**
 * of Taken `Confirmation` rows, so there is no transformation to apply here.
 *
 * **This is explicitly NOT `ComputeAdherenceUseCase`** — the BR-008 rolling-30-day-window Adherence
 * percentage (UC-009) is M-005. No `Dose` denominator, no window arithmetic, no percentage.
 */
class ObserveConfirmedDoseCountUseCase
    @Inject
    constructor(
        private val repository: ConfirmationRepository,
    ) {
        operator fun invoke(): Flow<Int> = repository.observeConfirmedDoseCount()
    }

package io.nemopill.adherencetracking.infrastructure

import androidx.room.TypeConverter
import io.nemopill.core.confirm.ConfirmationSource
import io.nemopill.core.confirm.ConfirmationStatus
import java.time.Instant

/**
 * Room `TypeConverter`s for the `Confirmation` row (file 04 § Local persistence — `Instant` stored
 * as UTC epoch millis; the two enums stored by `name`). Registered on `NemoPillDatabase` via
 * `@TypeConverters`. Pure mapping, no I/O.
 */
class ConfirmationConverters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant): Long = value.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun statusToName(value: ConfirmationStatus): String = value.name

    @TypeConverter
    fun nameToStatus(value: String): ConfirmationStatus = ConfirmationStatus.valueOf(value)

    @TypeConverter
    fun sourceToName(value: ConfirmationSource): String = value.name

    @TypeConverter
    fun nameToSource(value: String): ConfirmationSource = ConfirmationSource.valueOf(value)
}

package io.nemopill.adherencetracking.infrastructure

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room DAO for the `confirmation` table — an adapter implementation detail of
 * [RoomConfirmationRepository] (file 04 § ConfirmationRepository). No Domain / Application code
 * depends on it.
 *
 * [upsert] is the idempotent insert-or-replace keyed on the unique `doseId` index: a second
 * confirm for the same Dose replaces the conflicting row, so the table stays 1:1-on-`doseId`
 * (file 06 § Idempotency rule — safe against double-tap). [findByDoseId] backs the test/assert
 * read path; the production confirm flow does not read back.
 */
@Dao
interface ConfirmationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConfirmationEntity)

    @Query("SELECT * FROM confirmation WHERE doseId = :doseId LIMIT 1")
    suspend fun findByDoseId(doseId: String): ConfirmationEntity?

    @Query("SELECT COUNT(*) FROM confirmation")
    suspend fun count(): Int
}

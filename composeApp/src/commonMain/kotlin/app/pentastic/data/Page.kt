package app.pentastic.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Entity
data class Page(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = Uuid.random().toString(),
    val name: String = "Page",
    val orderAt: Long = Clock.System.now().toEpochMilliseconds(),

    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
)

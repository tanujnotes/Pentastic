package app.pentastic.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Page::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pageId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = Uuid.random().toString(),
    val pageId: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val priority: Int = 0,
    val text: String,
    val done: Boolean = false,
    val orderAt: Long = Clock.System.now().toEpochMilliseconds(),

    @ColumnInfo(defaultValue = "0")
    val repeatFrequency: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val repeatTaskStartFrom: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val taskLastDoneAt: Long = 0,

    @ColumnInfo(defaultValue = "0")
    val reminderAt: Long = 0,  // Timestamp when reminder should fire (0 = no reminder)
    @ColumnInfo(defaultValue = "0")
    val reminderEnabled: Int = 0,  // 0 = disabled, 1 = enabled

    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),

    @ColumnInfo(defaultValue = "0")
    val deletedAt: Long = 0,
)

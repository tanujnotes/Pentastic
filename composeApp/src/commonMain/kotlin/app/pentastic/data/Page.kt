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
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class Page(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = Uuid.random().toString(),
    val name: String = "Page",
    @ColumnInfo(defaultValue = "NULL")
    val parentId: Long? = null,
    val orderAt: Long = Clock.System.now().toEpochMilliseconds(),

    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),

    @ColumnInfo(defaultValue = "0")
    val deletedAt: Long = 0,

    @ColumnInfo(defaultValue = "0")
    val archivedAt: Long = 0,
)

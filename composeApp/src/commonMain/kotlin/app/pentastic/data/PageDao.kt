package app.pentastic.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: Page): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePage(page: Page)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePages(page: List<Page>)

    @Query("SELECT * FROM page WHERE deletedAt = 0 AND archivedAt = 0 ORDER BY orderAt")
    fun getAllPages(): Flow<List<Page>>

    @Query("SELECT * FROM page WHERE parentId IS NULL AND deletedAt = 0 AND archivedAt = 0 ORDER BY orderAt")
    fun getRootPages(): Flow<List<Page>>

    @Query("SELECT * FROM page WHERE parentId = :parentId AND deletedAt = 0 AND archivedAt = 0 ORDER BY orderAt")
    fun getSubPages(parentId: Long): Flow<List<Page>>

    @Query("SELECT * FROM page WHERE id = :id")
    suspend fun getPageById(id: Long): Page?

    @Query("DELETE FROM page WHERE id = :id")
    suspend fun deletePage(id: Long)

    @Query("UPDATE page SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeletePage(id: Long, deletedAt: Long)

    @Query("UPDATE page SET deletedAt = :deletedAt WHERE parentId = :parentId AND deletedAt = 0")
    suspend fun softDeleteSubPages(parentId: Long, deletedAt: Long)

    @Query("UPDATE page SET deletedAt = 0 WHERE id = :id")
    suspend fun restorePage(id: Long)

    @Query("UPDATE page SET deletedAt = 0 WHERE parentId = :parentId")
    suspend fun restoreSubPages(parentId: Long)

    @Query("SELECT * FROM page WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun getTrashedPages(): Flow<List<Page>>

    @Query("DELETE FROM page WHERE deletedAt > 0")
    suspend fun permanentlyDeleteAllTrashedPages()

    @Query("UPDATE page SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun archivePage(id: Long, archivedAt: Long)

    @Query("UPDATE page SET archivedAt = :archivedAt WHERE parentId = :parentId AND archivedAt = 0")
    suspend fun archiveSubPages(parentId: Long, archivedAt: Long)

    @Query("UPDATE page SET archivedAt = 0 WHERE id = :id")
    suspend fun unarchivePage(id: Long)

    @Query("UPDATE page SET archivedAt = 0 WHERE parentId = :parentId")
    suspend fun unarchiveSubPages(parentId: Long)

    @Query("SELECT * FROM page WHERE archivedAt > 0 AND deletedAt = 0 AND parentId IS NULL ORDER BY archivedAt DESC")
    fun getArchivedPages(): Flow<List<Page>>

    @Query("SELECT * FROM page WHERE archivedAt > 0 AND deletedAt = 0 AND parentId = :parentId ORDER BY orderAt")
    fun getArchivedSubPages(parentId: Long): Flow<List<Page>>
}

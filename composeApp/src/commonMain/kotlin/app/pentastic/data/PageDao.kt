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

    @Query("SELECT * FROM page ORDER BY orderAt")
    fun getAllPages(): Flow<List<Page>>

    @Query("SELECT * FROM page WHERE parentId IS NULL ORDER BY orderAt")
    fun getRootPages(): Flow<List<Page>>

    @Query("SELECT * FROM page WHERE parentId = :parentId ORDER BY orderAt")
    fun getSubPages(parentId: Long): Flow<List<Page>>

    @Query("SELECT * FROM page WHERE id = :id")
    suspend fun getPageById(id: Long): Page?

    @Query("DELETE FROM page WHERE id = :id")
    suspend fun deletePage(id: Long)
}

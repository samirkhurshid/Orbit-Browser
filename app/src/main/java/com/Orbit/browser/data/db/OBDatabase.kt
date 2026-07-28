package com.orbit.browser.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val visitCount: Int = 1,
    val favicon: ByteArray? = null,
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val favicon: ByteArray? = null,
    val folder: String = "Default",
    val addedAt: Long  = System.currentTimeMillis(),
    val sortOrder: Int = 0,
)

@Entity(tableName = "quick_access")
data class QuickAccessSite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val emoji: String  = "🌐",
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
)

@Entity(tableName = "frequent_sites")
data class FrequentSite(
    @PrimaryKey val url: String,
    val title: String,
    val visitCount: Int = 0,
    val lastVisited: Long = System.currentTimeMillis(),
    val favicon: ByteArray? = null,
)

@Entity(tableName = "downloads")
data class Download(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val filename: String,
    val mimeType: String,
    val filePath: String,
    val sizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.Pending,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val etaSeconds: Long = 0L,
    val speedBytesPerSec: Long = 0L,
)

enum class DownloadStatus { Pending, Downloading, Paused, Completed, Failed, Cancelled }

@Entity(tableName = "search_suggestions")
data class SearchSuggestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val usedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 1,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history WHERE url LIKE :query OR title LIKE :query ORDER BY visitedAt DESC LIMIT 20")
    suspend fun search(query: String): List<HistoryEntry>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): HistoryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry)

    @Query("UPDATE history SET visitCount = visitCount + 1, visitedAt = :now WHERE url = :url")
    suspend fun incrementVisit(url: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM history WHERE visitedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Delete
    suspend fun delete(entry: HistoryEntry)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY sortOrder, addedAt DESC")
    fun getAll(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE folder = :folder ORDER BY sortOrder")
    fun getByFolder(folder: String): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE url LIKE :query OR title LIKE :query LIMIT 20")
    suspend fun search(query: String): List<Bookmark>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isBookmarked(url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark)

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("UPDATE bookmarks SET sortOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Int)
}

@Dao
interface QuickAccessDao {
    @Query("SELECT * FROM quick_access ORDER BY sortOrder")
    fun getAll(): Flow<List<QuickAccessSite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: QuickAccessSite)

    @Delete
    suspend fun delete(site: QuickAccessSite)

    @Query("SELECT COUNT(*) FROM quick_access")
    suspend fun getCount(): Int

    @Query("UPDATE quick_access SET sortOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Int)
}

@Dao
interface FrequentSiteDao {
    @Query("SELECT * FROM frequent_sites ORDER BY visitCount DESC LIMIT 12")
    fun getTop(): Flow<List<FrequentSite>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(site: FrequentSite)

    @Query("UPDATE frequent_sites SET visitCount = visitCount + 1, lastVisited = :now WHERE url = :url")
    suspend fun incrementVisit(url: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM frequent_sites")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY startedAt DESC")
    fun getAll(): Flow<List<Download>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download): Long

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): Download?

    @Query("UPDATE downloads SET status = :status, downloadedBytes = :bytes, etaSeconds = :etaSec, speedBytesPerSec = :speedBytesSec, completedAt = :completedAt WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        status: DownloadStatus,
        bytes: Long,
        etaSec: Long = 0L,
        speedBytesSec: Long = 0L,
        completedAt: Long? = null
    )

    @Query("UPDATE downloads SET filename = :newFilename, filePath = :newPath WHERE id = :id")
    suspend fun updateFileNameAndPath(id: Long, newFilename: String, newPath: String)

    @Delete
    suspend fun delete(download: Download)
}

@Dao
interface SearchSuggestionDao {
    @Query("SELECT * FROM search_suggestions WHERE query LIKE :prefix ORDER BY useCount DESC LIMIT 8")
    suspend fun getSuggestions(prefix: String): List<SearchSuggestion>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(suggestion: SearchSuggestion)

    @Query("UPDATE search_suggestions SET useCount = useCount + 1, usedAt = :now WHERE query = :query")
    suspend fun incrementUse(query: String, now: Long = System.currentTimeMillis())
}

@Database(
    entities = [
        HistoryEntry::class,
        Bookmark::class,
        QuickAccessSite::class,
        FrequentSite::class,
        Download::class,
        SearchSuggestion::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class OBDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun quickAccessDao(): QuickAccessDao
    abstract fun frequentSiteDao(): FrequentSiteDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchSuggestionDao(): SearchSuggestionDao
}

package com.orbit.browser.security.vault

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// PasswordVaultDatabase
//
// Deliberately a SEPARATE Room database from OBDatabase (which holds history,
// bookmarks, downloads, etc.). This is a defense-in-depth choice, not an
// accident: keeping secrets in their own database file means:
//   - a bug or injection issue touching general browsing data has no direct
//     path to the vault's table
//   - the vault file can carry its own, stricter backup-exclusion rules
//   - the vault's schema/migrations evolve independently of browsing data
//
// Passwords are NEVER stored in plaintext. [encryptedPassword] and [iv] are
// produced by PasswordCipher (AES-256-GCM, key held in the Android Keystore).
// The plaintext password exists only transiently in memory when the user
// explicitly reveals it (after a biometric/device-credential prompt).
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "saved_logins")
data class SavedLoginEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val site: String,               // normalized host, e.g. "github.com"
    val siteDisplayUrl: String,     // full URL at time of save, for display
    val username: String,
    val encryptedPassword: ByteArray,
    val iv: ByteArray,
    val savedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
) {
    // Room/data-class default equals/hashCode compare ByteArray by reference,
    // not content — override so list diffing (Flow<List<...>> in Compose)
    // behaves correctly instead of treating every emission as "all changed".
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedLoginEntity) return false
        return id == other.id && site == other.site && username == other.username &&
            encryptedPassword.contentEquals(other.encryptedPassword) &&
            iv.contentEquals(other.iv) && savedAt == other.savedAt && lastUsedAt == other.lastUsedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + site.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + encryptedPassword.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + savedAt.hashCode()
        result = 31 * result + lastUsedAt.hashCode()
        return result
    }
}

@Dao
interface SavedLoginDao {
    @Query("SELECT * FROM saved_logins ORDER BY lastUsedAt DESC")
    fun observeAll(): Flow<List<SavedLoginEntity>>

    @Query("SELECT * FROM saved_logins WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SavedLoginEntity?

    @Query("SELECT * FROM saved_logins WHERE site = :site")
    suspend fun getForSite(site: String): List<SavedLoginEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_logins WHERE site = :site AND username = :username)")
    suspend fun exists(site: String, username: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SavedLoginEntity): Long

    @Query("UPDATE saved_logins SET lastUsedAt = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM saved_logins WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_logins")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM saved_logins")
    suspend fun count(): Int
}

@Database(entities = [SavedLoginEntity::class], version = 1, exportSchema = false)
abstract class PasswordVaultDatabase : RoomDatabase() {
    abstract fun savedLoginDao(): SavedLoginDao
}

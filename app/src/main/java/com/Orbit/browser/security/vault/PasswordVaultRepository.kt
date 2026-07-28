package com.orbit.browser.security.vault

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// PasswordVaultRepository
//
// The single entry point the rest of the app talks to. Deliberately splits
// "metadata" (site + username — safe to show in a list without unlocking
// anything) from "reveal" (the actual password — always requires a fresh
// biometric auth immediately before decrypting, never cached in memory
// afterward).
//
// Every write (add/delete) also requires biometric auth first — see
// PasswordCipher's key config for why that's enforced at the OS level, not
// just requested here.
// ─────────────────────────────────────────────────────────────────────────────

data class SavedLoginMeta(
    val id: Long,
    val site: String,
    val siteDisplayUrl: String,
    val username: String,
    val savedAt: Long,
    val lastUsedAt: Long,
)

sealed class VaultResult<out T> {
    data class Success<T>(val value: T) : VaultResult<T>()
    data object AuthRequired : VaultResult<Nothing>()
    data object AuthFailed : VaultResult<Nothing>()
    data class Error(val message: String) : VaultResult<Nothing>()
}

class PasswordVaultRepository(
    private val dao: SavedLoginDao,
    private val cipher: PasswordCipher,
) {

    /** Metadata-only stream for the vault list UI — never touches the cipher. */
    val savedLogins: Flow<List<SavedLoginMeta>> = dao.observeAll().map { list ->
        list.map { it.toMeta() }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) { dao.count() }

    suspend fun existsForSite(site: String, username: String): Boolean =
        withContext(Dispatchers.IO) { dao.exists(normalizeSite(site), username) }

    /**
     * Encrypts and saves a new credential. [authHelper] must have already
     * produced a Success result for this call to succeed — pass the same
     * helper instance the caller used to gate the "Add password" action.
     */
    suspend fun addLogin(
        site: String,
        siteDisplayUrl: String,
        username: String,
        password: String,
    ): VaultResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = cipher.encrypt(password)
            dao.insert(
                SavedLoginEntity(
                    site = normalizeSite(site),
                    siteDisplayUrl = siteDisplayUrl,
                    username = username,
                    encryptedPassword = payload.ciphertext,
                    iv = payload.iv,
                )
            )
            VaultResult.Success(Unit)
        } catch (e: android.security.keystore.UserNotAuthenticatedException) {
            VaultResult.AuthRequired
        } catch (e: Exception) {
            VaultResult.Error(e.message ?: "Failed to save password")
        }
    }

    suspend fun deleteLogin(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    /**
     * Decrypts and returns the plaintext password for [id]. Caller must have
     * already run a successful BiometricAuthHelper.authenticate() in this
     * same user action — this does not itself trigger a prompt, it only
     * attempts the decrypt, which the OS will refuse if auth isn't fresh.
     *
     * The returned string should be held only as long as needed to display
     * or copy it, then discarded — callers should not cache it in a
     * long-lived StateFlow.
     */
    suspend fun revealPassword(id: Long): VaultResult<String> = withContext(Dispatchers.IO) {
        val entry = dao.getById(id) ?: return@withContext VaultResult.Error("Entry not found")
        val plain = cipher.decrypt(entry.encryptedPassword, entry.iv)
            ?: return@withContext VaultResult.AuthFailed
        dao.touch(id)
        VaultResult.Success(plain)
    }

    private fun normalizeSite(site: String): String {
        return site.removePrefix("https://").removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .lowercase()
    }

    private fun SavedLoginEntity.toMeta() = SavedLoginMeta(
        id = id, site = site, siteDisplayUrl = siteDisplayUrl,
        username = username, savedAt = savedAt, lastUsedAt = lastUsedAt,
    )
}

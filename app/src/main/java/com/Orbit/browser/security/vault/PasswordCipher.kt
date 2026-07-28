package com.orbit.browser.security.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// ─────────────────────────────────────────────────────────────────────────────
// PasswordCipher — AES-256-GCM encrypt/decrypt backed by the Android Keystore.
//
// The AES key is generated inside the device's secure hardware (StrongBox or
// TEE, whichever the device supports) and is marked non-exportable — there is
// no API to read the raw key bytes out of the Keystore. encrypt()/decrypt()
// are the only way to use it. The key never exists in app memory, in a heap
// dump, or on disk in any extractable form.
//
// setUserAuthenticationRequired(true) means the key itself is LOCKED at the
// OS level until the user authenticates (biometric or device credential)
// within [AUTH_VALIDITY_SECONDS] of the Cipher being requested — this isn't
// just an app-level "ask nicely" prompt, Android will throw
// UserNotAuthenticatedException if you try to use the key without a fresh
// enough auth. That's the real guarantee: even a rooted device reading app
// memory can't decrypt without the user's biometric/PIN having been entered
// recently.
// ─────────────────────────────────────────────────────────────────────────────

class PasswordCipher {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS        = "orbit_password_vault_key_v1"
        private const val TRANSFORMATION   = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH   = 128 // bits
        private const val AUTH_VALIDITY_SECONDS = 30 // how long after biometric auth the key stays usable
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                AUTH_VALIDITY_SECONDS,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts [plaintext]. Requires a recent biometric/device-credential
     * auth (same as [decrypt]) since the key itself is auth-gated — saving a
     * new password is treated with the same care as revealing one.
     *
     * Throws android.security.keystore.UserNotAuthenticatedException if no
     * recent authentication is on file — callers must run this only after
     * BiometricAuthHelper confirms success.
     */
    fun encrypt(plaintext: String): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(ciphertext = ciphertext, iv = cipher.iv)
    }

    /**
     * Decrypts [ciphertext] using the stored [iv]. Returns null if the key
     * is unavailable, unauthenticated, or the data is corrupt/tampered —
     * never throws out to the caller, so a UI can show a clean "couldn't
     * unlock" state instead of crashing.
     */
    fun decrypt(ciphertext: ByteArray, iv: ByteArray): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    data class EncryptedPayload(val ciphertext: ByteArray, val iv: ByteArray)
}

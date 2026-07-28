package com.orbit.browser.security.vault

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// ─────────────────────────────────────────────────────────────────────────────
// BiometricAuthHelper — thin coroutine wrapper around androidx.biometric.
//
// Every vault read/write goes through here first. The result isn't just a
// UX gate: the Keystore key backing PasswordCipher is itself configured with
// setUserAuthenticationRequired(true), so a successful prompt here is what
// actually unlocks the ability to call Cipher.init() at all — this class
// isn't "trust me, they scanned a finger", it's the real authorization step.
// ─────────────────────────────────────────────────────────────────────────────

sealed class BiometricAuthResult {
    data object Success : BiometricAuthResult()
    data object Cancelled : BiometricAuthResult()
    data class Error(val message: String) : BiometricAuthResult()
    data object NoHardwareOrNotEnrolled : BiometricAuthResult()
}

class BiometricAuthHelper(private val activity: FragmentActivity) {

    /** True if the device has usable biometric or device-credential auth set up. */
    fun canAuthenticate(): Boolean {
        val manager = BiometricManager.from(activity)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the system biometric/device-credential prompt and suspends until
     * the user succeeds, cancels, or an error occurs. Safe to call from a
     * ViewModel coroutine — this does not block the calling thread.
     */
    suspend fun authenticate(
        title: String = "Unlock Password Vault",
        subtitle: String = "Confirm it's you to view or save a password",
    ): BiometricAuthResult = suspendCancellableCoroutine { continuation ->

        if (!canAuthenticate()) {
            continuation.resume(BiometricAuthResult.NoHardwareOrNotEnrolled)
            return@suspendCancellableCoroutine
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!continuation.isActive) return
                val outcome = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> BiometricAuthResult.Cancelled
                    else -> BiometricAuthResult.Error(errString.toString())
                }
                continuation.resume(outcome)
            }

            // Note: onAuthenticationFailed (wrong fingerprint) fires per-attempt,
            // not a terminal state — the prompt stays open for retry, so we
            // deliberately don't resume the continuation here.
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)

        continuation.invokeOnCancellation {
            prompt.cancelAuthentication()
        }
    }
}

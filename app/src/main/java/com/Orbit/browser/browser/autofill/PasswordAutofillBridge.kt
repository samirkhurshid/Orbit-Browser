package com.orbit.browser.browser.autofill

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.orbit.browser.security.vault.PasswordVaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PasswordAutofillBridge
 *
 * Handles JavaScript injection into WebViews to detect password form submissions
 * and offer native credential save and autofill prompts.
 */
class PasswordAutofillBridge(
    private val repository: PasswordVaultRepository,
    private val scope: CoroutineScope,
    private val onPromptSavePassword: (site: String, username: String, password: String) -> Unit,
) {

    companion object {
        const val JS_INTERFACE_NAME = "OrbitPasswordBridge"

        const val AUTOFILL_DETECTION_SCRIPT = """
            (function() {
                if (window.__orbitPasswordScriptInjected) return;
                window.__orbitPasswordScriptInjected = true;

                document.addEventListener('submit', function(e) {
                    try {
                        var form = e.target;
                        var passwordInput = form.querySelector('input[type="password"]');
                        if (!passwordInput || !passwordInput.value) return;

                        var userInput = form.querySelector('input[type="text"], input[type="email"], input[type="username"]');
                        var username = userInput ? userInput.value : '';
                        var password = passwordInput.value;

                        if (password && window.OrbitPasswordBridge) {
                            window.OrbitPasswordBridge.onFormSubmitted(window.location.hostname, username, password);
                        }
                    } catch(err) {}
                }, true);
            })();
        """
    }

    @JavascriptInterface
    fun onFormSubmitted(site: String, username: String, password: String) {
        if (site.isBlank() || username.isBlank() || password.isBlank()) return
        scope.launch(Dispatchers.Main) {
            val exists = withContext(Dispatchers.IO) {
                repository.existsForSite(site, username)
            }
            if (!exists) {
                onPromptSavePassword(site, username, password)
            }
        }
    }

    /**
     * Injects JS form listener into WebView when page finishes loading.
     */
    fun injectFormListener(webView: WebView) {
        webView.evaluateJavascript(AUTOFILL_DETECTION_SCRIPT, null)
    }

    /**
     * Injects autofill credentials into active form fields.
     */
    fun autofillCredentials(webView: WebView, username: String, password: String) {
        val safeUser = username.replace("'", "\\'")
        val safePass = password.replace("'", "\\'")
        val script = """
            (function() {
                var passInput = document.querySelector('input[type="password"]');
                if (passInput) {
                    passInput.value = '$safePass';
                    passInput.dispatchEvent(new Event('input', { bubbles: true }));
                    passInput.dispatchEvent(new Event('change', { bubbles: true }));
                }
                var userInput = document.querySelector('input[type="text"], input[type="email"], input[type="username"]');
                if (userInput) {
                    userInput.value = '$safeUser';
                    userInput.dispatchEvent(new Event('input', { bubbles: true }));
                    userInput.dispatchEvent(new Event('change', { bubbles: true }));
                }
            })();
        """
        webView.evaluateJavascript(script, null)
    }
}

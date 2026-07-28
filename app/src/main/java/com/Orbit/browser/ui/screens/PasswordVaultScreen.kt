package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.orbit.browser.security.vault.BiometricAuthHelper
import com.orbit.browser.security.vault.BiometricAuthResult
import com.orbit.browser.security.vault.SavedLoginMeta
import com.orbit.browser.security.vault.VaultResult
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.components.FrostedBackButton
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.theme.LocalOBTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// PasswordVaultScreen
//
// Security-relevant UX decisions, worth reading before touching this file:
//  - Every save AND every reveal requires a fresh biometric/device-credential
//    prompt. This isn't just a UI gate — PasswordCipher's Keystore key is
//    itself configured to require it, so there's no code path that decrypts
//    without the OS having verified the user first.
//  - Only one password can be "revealed" at a time, and it auto-hides after
//    REVEAL_TIMEOUT_MS. This limits how long a decrypted secret sits in
//    Compose state / on screen if the user walks away.
//  - The vault list itself (site + username) is NOT gated — seeing that you
//    have a saved login for github.com isn't sensitive the way the password
//    itself is, and gating the whole screen would just train users to leave
//    it unlocked in the background.
// ─────────────────────────────────────────────────────────────────────────────

private const val REVEAL_TIMEOUT_MS = 15_000L

@Composable
fun PasswordVaultScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val theme     = LocalOBTheme.current
    val g         = theme.glass
    val isDark    = theme.isDark
    val a1        = theme.effectiveA1
    val context   = LocalContext.current
    val activity  = context as? FragmentActivity
    val scope     = rememberCoroutineScope()

    val savedLogins by viewModel.savedLogins.collectAsState()

    val authHelper = remember(activity) { activity?.let { BiometricAuthHelper(it) } }
    val hardwareReady = remember(authHelper) { authHelper?.canAuthenticate() ?: false }

    var revealedId: Long? by remember { mutableStateOf(null) }
    var revealedPlaintext: String? by remember { mutableStateOf(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var errorMessage: String? by remember { mutableStateOf(null) }

    // Prevent screenshots and screen recording while Password Vault screen is active
    DisposableEffect(activity) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Auto-hide any revealed password after a short window.
    LaunchedEffect(revealedId) {
        if (revealedId != null) {
            delay(REVEAL_TIMEOUT_MS)
            revealedId = null
            revealedPlaintext = null
        }
    }

    fun requestReveal(entry: SavedLoginMeta) {
        val helper = authHelper ?: return
        scope.launch {
            when (val auth = helper.authenticate(subtitle = "Confirm it's you to view this password")) {
                is BiometricAuthResult.Success -> {
                    when (val result = viewModel.revealPassword(entry.id)) {
                        is VaultResult.Success -> {
                            revealedId = entry.id
                            revealedPlaintext = result.value
                        }
                        is VaultResult.AuthFailed -> errorMessage = "Couldn't unlock this entry. Try again."
                        is VaultResult.Error -> errorMessage = result.message
                        else -> Unit
                    }
                }
                is BiometricAuthResult.Cancelled -> Unit
                is BiometricAuthResult.NoHardwareOrNotEnrolled ->
                    errorMessage = "Set up a screen lock or fingerprint to use the password vault."
                is BiometricAuthResult.Error -> errorMessage = auth.message
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FrostedBackButton(onClick = { viewModel.closePasswords() }, isDark = isDark)
                Text(
                    text = "Passwords",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = g.txColor,
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                )
                if (hardwareReady) {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add password", tint = a1)
                    }
                }
            }

            when {
                !hardwareReady -> NoBiometricNotice(modifier = Modifier.weight(1f))
                savedLogins.isEmpty() -> EmptyVaultNotice(modifier = Modifier.weight(1f))
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(savedLogins, key = { it.id }) { entry ->
                        SavedLoginRow(
                            entry = entry,
                            isRevealed = revealedId == entry.id,
                            revealedPassword = if (revealedId == entry.id) revealedPlaintext else null,
                            onReveal = { requestReveal(entry) },
                            onHide = { revealedId = null; revealedPlaintext = null },
                            onDelete = { viewModel.deletePassword(entry.id) },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog && authHelper != null) {
        AddPasswordDialog(
            onDismiss = { showAddDialog = false },
            onSave = { site, username, password ->
                scope.launch {
                    when (val auth = authHelper.authenticate(subtitle = "Confirm it's you to save this password")) {
                        is BiometricAuthResult.Success -> {
                            val result = viewModel.addPassword(
                                site = site, siteDisplayUrl = site, username = username, password = password,
                            )
                            when (result) {
                                is VaultResult.Success -> showAddDialog = false
                                is VaultResult.AuthRequired -> errorMessage = "Authentication expired, try again."
                                is VaultResult.Error -> errorMessage = result.message
                                else -> Unit
                            }
                        }
                        is BiometricAuthResult.Cancelled -> Unit
                        is BiometricAuthResult.NoHardwareOrNotEnrolled ->
                            errorMessage = "Set up a screen lock or fingerprint to use the password vault."
                        is BiometricAuthResult.Error -> errorMessage = auth.message
                    }
                }
            },
        )
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            },
            title = { Text("Password Vault") },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun SavedLoginRow(
    entry: SavedLoginMeta,
    isRevealed: Boolean,
    revealedPassword: String?,
    onReveal: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
) {
    val theme = LocalOBTheme.current
    val g     = theme.glass
    val a1    = theme.effectiveA1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .frostedGlass(isDark = theme.isDark, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(a1.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = a1, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(entry.site, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = g.txColor,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.username, fontSize = 11.5.sp, color = g.tx2Color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            AnimatedVisibility(visible = isRevealed && revealedPassword != null) {
                Text(
                    text = revealedPassword ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = a1,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        IconButton(onClick = { if (isRevealed) onHide() else onReveal() }) {
            Icon(
                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (isRevealed) "Hide password" else "Show password",
                tint = g.tx2Color,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = g.tx2Color)
        }
    }
}

@Composable
private fun AddPasswordDialog(
    onDismiss: () -> Unit,
    onSave: (site: String, username: String, password: String) -> Unit,
) {
    var site by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = site, onValueChange = { site = it },
                    label = { Text("Website") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username or email") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") }, singleLine = true,
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = site.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                onClick = { onSave(site.trim(), username.trim(), password) },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EmptyVaultNotice(modifier: Modifier = Modifier) {
    val theme = LocalOBTheme.current
    val g = theme.glass
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = g.tx2Color, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(14.dp))
            Text("No Saved Passwords", fontSize = 18.sp, fontWeight = FontWeight.Black, color = g.txColor)
            Spacer(Modifier.height(6.dp))
            Text(
                "Passwords you save are encrypted (AES-256) and locked behind your fingerprint or PIN.",
                fontSize = 12.sp, color = g.tx2Color,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NoBiometricNotice(modifier: Modifier = Modifier) {
    val theme = LocalOBTheme.current
    val g = theme.glass
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LockOpen, contentDescription = null, tint = g.tx2Color, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(14.dp))
            Text("Screen Lock Required", fontSize = 18.sp, fontWeight = FontWeight.Black, color = g.txColor)
            Spacer(Modifier.height(6.dp))
            Text(
                "The password vault only works with a fingerprint, face unlock, or PIN set up on this device — that's what encrypts and protects your saved passwords.",
                fontSize = 12.sp, color = g.tx2Color,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

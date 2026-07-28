package com.orbit.browser.security.adblock

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdBlocker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val exactBlockDomains = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var containsRules: List<String> = emptyList()
    @Volatile private var regexRules: List<Pattern> = emptyList()
    private val whitelistDomains  = ConcurrentHashMap.newKeySet<String>()

    private var isLoaded = false
    private val loadLock = Object()

    private val hardcodedBlockDomains = setOf(
        "doubleclick.net", "googlesyndication.com", "googletagmanager.com",
        "googletagservices.com", "adservice.google.com", "pagead2.googlesyndication.com",
        "adnxs.com", "rubiconproject.com", "pubmatic.com", "openx.net",
        "smartadserver.com", "advertising.com",
        "outbrain.com", "taboola.com", "revcontent.com",
        "scorecardresearch.com", "quantserve.com", "comscore.com",
        "chartbeat.com", "newrelic.com", "bugsnag.com",
        "hotjar.com", "mouseflow.com", "fullstory.com", "logrocket.com",
        "segment.com", "amplitude.com", "mixpanel.com",
        "connect.facebook.net",
        "platform.twitter.com", "static.ads-twitter.com",
        "coinhive.com", "minero.cc", "cryptoloot.pro",
    )

    suspend fun initialize() = withContext(Dispatchers.IO) {
        synchronized(loadLock) { if (isLoaded) return@withContext }

        exactBlockDomains.addAll(hardcodedBlockDomains)

        val newContains = mutableListOf<String>()
        val newRegex    = mutableListOf<Pattern>()

        try {
            context.assets.open("adblock/easylist.txt").use { stream ->
                BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                    parseLine(line.trim(), newContains, newRegex)
                }
            }
        } catch (e: Exception) { /* asset optional */ }

        try {
            context.assets.open("adblock/easyprivacy.txt").use { stream ->
                BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                    parseLine(line.trim(), newContains, newRegex)
                }
            }
        } catch (e: Exception) { /* asset optional */ }

        containsRules = newContains.toList()
        regexRules    = newRegex.toList()

        synchronized(loadLock) { isLoaded = true }
    }

    private fun parseLine(
        line: String,
        outContains: MutableList<String>,
        outRegex: MutableList<Pattern>,
    ) {
        when {
            line.isEmpty()        -> return
            line.startsWith("!")  -> return
            line.startsWith("[")  -> return
            line.startsWith("@@") -> {
                val domain = line.removePrefix("@@").removeSurrounding("||", "^")
                if (domain.isNotEmpty()) whitelistDomains.add(domain)
            }
            line.startsWith("||") -> {
                val rule = line.removePrefix("||").removeSuffix("^")
                    .substringBefore("^").substringBefore("/")
                if (rule.isNotEmpty() && !rule.contains("*")) exactBlockDomains.add(rule)
            }
            line.startsWith("/") && line.endsWith("/") -> {
                try {
                    val pattern = line.drop(1).dropLast(1)
                    outRegex.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                } catch (_: Exception) {}
            }
            line.contains("*") -> {
                val simplified = line.replace("*", "")
                if (simplified.length > 3) outContains.add(simplified.lowercase(Locale.ROOT))
            }
        }
    }

    fun shouldBlock(url: String, pageUrl: String): Boolean {
        if (url.isEmpty()) return false

        val uri  = try { Uri.parse(url) } catch (_: Exception) { return false }
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false

        val pageHost = try {
            Uri.parse(pageUrl).host?.lowercase(Locale.ROOT) ?: ""
        } catch (_: Exception) { "" }
        if (host == pageHost || host.endsWith(".$pageHost")) return false

        if (whitelistDomains.any { host == it || host.endsWith(".$it") }) return false
        if (exactBlockDomains.any { host == it || host.endsWith(".$it") }) return true

        val lowerUrl = url.lowercase(Locale.ROOT)
        if (containsRules.any { lowerUrl.contains(it) }) return true
        if (regexRules.any { it.matcher(url).find() }) return true

        return false
    }

    fun isReady() = isLoaded
}

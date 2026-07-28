package com.orbit.browser.security.dns

import okhttp3.*
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureDnsResolver @Inject constructor() : Dns {

    private val dohClient = OkHttpClient.Builder()
        .dns(Dns.SYSTEM)
        .build()

    companion object {
        private const val CLOUDFLARE_DOH = "https://cloudflare-dns.com/dns-query"
        private const val GOOGLE_DOH     = "https://dns.google/dns-query"
    }

    var isDohEnabled: Boolean = true

    override fun lookup(hostname: String): List<InetAddress> {
        if (!isDohEnabled) return Dns.SYSTEM.lookup(hostname)
        return try {
            queryDoH(hostname, CLOUDFLARE_DOH)
        } catch (e: Exception) {
            try {
                queryDoH(hostname, GOOGLE_DOH)
            } catch (e2: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    private fun queryDoH(hostname: String, server: String): List<InetAddress> {
        val url = "$server?name=${hostname}&type=A"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/dns-json")
            .get()
            .build()

        dohClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("DoH failed: ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty DoH response")
            val addresses = parseDoHResponse(body, hostname)
            if (addresses.isEmpty()) throw UnknownHostException(hostname)
            return addresses
        }
    }

    private fun parseDoHResponse(json: String, hostname: String): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        val answerSection = json.substringAfter("\"Answer\":[").substringBefore("]")
        val records = answerSection.split("},{")

        for (record in records) {
            if (record.contains("\"type\":1") || record.contains("\"type\":28")) {
                val data = record.substringAfter("\"data\":\"").substringBefore("\"")
                if (data.isNotEmpty() && !data.startsWith("{")) {
                    try { addresses.add(InetAddress.getByName(data)) } catch (_: Exception) {}
                }
            }
        }
        return addresses
    }
}

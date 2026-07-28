package com.orbit.browser.data.weather

import com.orbit.browser.ui.WeatherKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class HourlyForecast(val time: String, val temp: Int)

data class WeatherResult(
    val temperature: Float,
    val weatherCode: Int,
    val weatherKind: WeatherKind
)

@Singleton
class WeatherRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun getWeather(lat: Double, lon: Double): WeatherResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OrbitBrowser/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyStr = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful || bodyStr.isBlank()) return@withContext null

            val json = JSONObject(bodyStr)
            val currentWeather = json.optJSONObject("current_weather") ?: return@withContext null

            val temp = currentWeather.optDouble("temperature", 25.0).toFloat()
            val code = currentWeather.optInt("weathercode", 0)

            val kind = mapWmoCodeToKind(code)
            WeatherResult(temperature = temp, weatherCode = code, weatherKind = kind)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun mapWmoCodeToKind(code: Int): WeatherKind {
        return when (code) {
            0 -> WeatherKind.Clear
            1, 2, 3 -> WeatherKind.Cloudy
            45, 48 -> WeatherKind.Foggy
            51, 53, 55, 56, 57 -> WeatherKind.Drizzle
            61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.Rainy
            71, 73, 75, 77, 85, 86 -> WeatherKind.Snowing
            95, 96, 99 -> WeatherKind.Thunderstorm
            else -> WeatherKind.Clear
        }
    }
}

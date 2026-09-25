package com.climatact.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class WeatherData(
    val city: String,
    val country: String,
    val temperature: Double,
    val apparent: Double,
    val humidity: Int,
    val wind: Double,
    val windDirection: Int,
    val pressure: Double,
    val weatherCode: Int,
    val precipitationProbability: Int,
    val times: List<String>,
    val hourlyTemperatures: List<Double>,
    val hourlyRainProbability: List<Int>
)

private data class Place(val name: String, val latitude: Double, val longitude: Double, val country: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClimaTactApp() }
    }
}

@Composable
fun ClimaTactApp() {
    var screen by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("Lucknow") }
    var weather by remember { mutableStateOf<WeatherData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadWeather(city: String) {
        loading = true
        error = null
        Thread {
            try {
                val place = searchPlace(city)
                    ?: throw IllegalArgumentException("Location not found")
                val result = fetchWeather(place)
                runOnUiThread {
                    weather = result
                    query = result.city
                    loading = false
                }
            } catch (e: Exception) {
                runOnUiThread {
                    error = e.message ?: "Unable to load weather"
                    loading = false
                }
            }
        }.start()
    }

    LaunchedEffect(Unit) {
        loadWeather("Lucknow")
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF071018),
            surface = Color(0xFF0D1B24),
            primary = Color(0xFF62D8FF)
        )
    ) {
        Scaffold(
            containerColor = Color(0xFF071018),
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0B161E)) {
                    NavigationBarItem(screen == 0, { screen = 0 }, { Icon(Icons.Default.WbSunny, null) }, { Text("Weather") })
                    NavigationBarItem(screen == 1, { screen = 1 }, { Icon(Icons.Default.Radar, null) }, { Text("Atmosphere") })
                    NavigationBarItem(screen == 2, { screen = 2 }, { Icon(Icons.Default.Timeline, null) }, { Text("Climate") })
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("CLIMA-TACT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("LIVE WEATHER / ATMOSPHERE", color = Color(0xFF8EA5B4), style = MaterialTheme.typography.labelMedium)

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { if (query.isNotBlank()) loadWeather(query) }) {
                            Icon(Icons.Default.Refresh, null)
                        }
                    },
                    label = { Text("Location") },
                    singleLine = true
                )

                when {
                    loading -> LoadingCard()
                    error != null -> ErrorCard(error!!, onRetry = { loadWeather(query) })
                    weather != null -> when (screen) {
                        0 -> WeatherScreen(weather!!)
                        1 -> AtmosphereScreen(weather!!)
                        else -> ClimateScreen(weather!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF10232E), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator()
            Text("Fetching live atmospheric data…")
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF2A1719), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("WEATHER FEED UNAVAILABLE", fontWeight = FontWeight.Bold)
            Text(message, color = Color(0xFFD0BFC0))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun InfoCard(title: String, value: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF10232E), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF62D8FF), modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color(0xFF8EA5B4))
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(detail, color = Color(0xFFB4C4CC))
            }
        }
    }
}

@Composable
private fun WeatherScreen(data: WeatherData) {
    InfoCard("NOW • ${data.city.uppercase(Locale.getDefault())}", "${data.temperature.roundToInt()}°", "${weatherDescription(data.weatherCode)} • Feels like ${data.apparent.roundToInt()}°", weatherIcon(data.weatherCode))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard("HUMIDITY", "${data.humidity}%", Icons.Default.WaterDrop, Modifier.weight(1f))
        MetricCard("WIND", "${data.wind.roundToInt()} km/h", Icons.Default.Air, Modifier.weight(1f))
    }
    InfoCard("PRESSURE", "${data.pressure.roundToInt()} hPa", "Sea-level atmospheric pressure", Icons.Default.Speed)
    Text("NEXT 12 HOURS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    TemperatureChart(data.hourlyTemperatures.take(12))
}

@Composable
private fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF10232E), modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = Color(0xFF62D8FF))
            Text(title, color = Color(0xFF8EA5B4), style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TemperatureChart(values: List<Double>) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF0D1E27), modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Canvas(Modifier.fillMaxSize().padding(20.dp)) {
            if (values.size < 2) return@Canvas
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 1.0
            val range = (max - min).coerceAtLeast(1.0)
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = size.width * index / (values.lastIndex.coerceAtLeast(1))
                val y = size.height - ((value - min) / range).toFloat() * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = Color(0xFF62D8FF), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
            values.forEachIndexed { index, value ->
                val x = size.width * index / (values.lastIndex.coerceAtLeast(1))
                val y = size.height - ((value - min) / range).toFloat() * size.height
                drawCircle(Color(0xFFB9F0FF), 5f, Offset(x, y))
            }
        }
    }
}

@Composable
private fun AtmosphereScreen(data: WeatherData) {
    Text("ATMOSPHERIC VISUALIZER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0E2633),
        modifier = Modifier.fillMaxWidth().height(320.dp)
    ) {
        AtmosphericCanvas(data.weatherCode, data.precipitationProbability, data.windDirection)
    }
    InfoCard("RAIN PROBABILITY", "${data.precipitationProbability}%", "Current hourly precipitation probability", Icons.Default.WaterDrop)
    InfoCard("WIND DIRECTION", "${data.windDirection}°", compassDirection(data.windDirection), Icons.Default.Explore)
}

@Composable
private fun AtmosphericCanvas(weatherCode: Int, rainProbability: Int, windDirection: Int) {
    val transition = rememberInfiniteTransition(label = "atmosphere")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize().padding(16.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f - 10f)
            val base = if (weatherCode in 51..82 || weatherCode in 95..99) Color(0xFF54C8FF) else Color(0xFF8EE7FF)

            for (ring in 1..4) {
                val radius = 42f + ring * 32f + sin((phase * 6.283f) + ring) * 6f
                drawCircle(
                    color = base.copy(alpha = 0.13f),
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }

            val direction = Math.toRadians(windDirection.toDouble())
            val dx = cos(direction).toFloat()
            val dy = sin(direction).toFloat()

            for (i in 0 until 10) {
                val progress = (phase + i / 10f) % 1f
                val start = Offset(
                    center.x - dx * size.width * 0.55f + dx * size.width * progress,
                    center.y - dy * size.height * 0.55f + dy * size.height * progress
                )
                drawLine(
                    color = base.copy(alpha = 0.12f + progress * 0.35f),
                    start = start,
                    end = Offset(start.x + dx * 46f, start.y + dy * 46f),
                    strokeWidth = 3f
                )
            }

            if (rainProbability >= 20 || weatherCode in 51..67 || weatherCode in 80..82) {
                for (i in 0 until 55) {
                    val x = (i * 47f) % size.width
                    val y = ((phase + i / 55f) % 1f) * size.height
                    drawLine(
                        color = base.copy(alpha = 0.18f + rainProbability / 500f),
                        start = Offset(x, y),
                        end = Offset(x - 4f, y + 14f),
                        strokeWidth = 2f
                    )
                }
            }
        }

        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(weatherIcon(weatherCode), null, tint = Color(0xFFB9F0FF), modifier = Modifier.size(76.dp))
            Spacer(Modifier.height(8.dp))
            Text(weatherDescription(weatherCode), fontWeight = FontWeight.Bold)
            Text("Wind " + compassDirection(windDirection), color = Color(0xFF8EA5B4), style = MaterialTheme.typography.labelMedium)
        }

        Text(
            "LIVE ATMOSPHERIC FLOW",
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            color = Color(0xFF8EA5B4),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
@Composable
private fun ClimateScreen(data: WeatherData) {
    Text("CLIMATE CONTEXT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    InfoCard("CURRENT ATMOSPHERE", "${data.temperature.roundToInt()}°C", "Weather is an instantaneous state, not a climate trend.", Icons.Default.Timeline)
    InfoCard("HUMIDITY", "${data.humidity}%", "Relative humidity at 2 m", Icons.Default.WaterDrop)
    InfoCard("PRECIPITATION SIGNAL", "${data.precipitationProbability}%", "Short-term atmospheric probability, not a climate indicator.", Icons.Default.Cloud)
}

private fun searchPlace(name: String): Place? {
    val encoded = URLEncoder.encode(name.trim(), "UTF-8")
    val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
    val json = getJson(url)
    val results = json.optJSONArray("results") ?: return null
    if (results.length() == 0) return null
    val item = results.getJSONObject(0)
    return Place(
        item.optString("name", name),
        item.getDouble("latitude"),
        item.getDouble("longitude"),
        item.optString("country", "")
    )
}

private fun fetchWeather(place: Place): WeatherData {
    val url = URL(
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,pressure_msl,wind_speed_10m,wind_direction_10m" +
            "&hourly=temperature_2m,precipitation_probability" +
            "&forecast_days=2&timezone=auto"
    )
    val json = getJson(url)
    val current = json.getJSONObject("current")
    val hourly = json.getJSONObject("hourly")
    val timesArray = hourly.getJSONArray("time")
    val tempArray = hourly.getJSONArray("temperature_2m")
    val rainArray = hourly.getJSONArray("precipitation_probability")

    val times = mutableListOf<String>()
    val temps = mutableListOf<Double>()
    val rain = mutableListOf<Int>()
    for (i in 0 until minOf(24, timesArray.length())) {
        times += timesArray.getString(i)
        temps += tempArray.getDouble(i)
        rain += rainArray.getInt(i)
    }

    return WeatherData(
        city = place.name,
        country = place.country,
        temperature = current.getDouble("temperature_2m"),
        apparent = current.getDouble("apparent_temperature"),
        humidity = current.getInt("relative_humidity_2m"),
        wind = current.getDouble("wind_speed_10m"),
        windDirection = current.getInt("wind_direction_10m"),
        pressure = current.getDouble("pressure_msl"),
        weatherCode = current.getInt("weather_code"),
        precipitationProbability = rain.firstOrNull() ?: 0,
        times = times,
        hourlyTemperatures = temps,
        hourlyRainProbability = rain
    )
}

private fun getJson(url: URL): JSONObject {
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    connection.setRequestProperty("Accept", "application/json")
    return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }.also {
        connection.disconnect()
    }
}

private fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear sky"
    1, 2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rain"
    71, 73, 75 -> "Snow"
    80, 81, 82 -> "Rain showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Mixed conditions"
}

private fun weatherIcon(code: Int) = when (code) {
    0 -> Icons.Default.WbSunny
    1, 2 -> Icons.Default.CloudQueue
    3, 45, 48 -> Icons.Default.Cloud
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Default.WaterDrop
    95, 96, 99 -> Icons.Default.Thunderstorm
    else -> Icons.Default.Cloud
}

private fun compassDirection(degrees: Int): String = when ((degrees + 22) / 45 % 8) {
    0 -> "North"
    1 -> "Northeast"
    2 -> "East"
    3 -> "Southeast"
    4 -> "South"
    5 -> "Southwest"
    6 -> "West"
    else -> "Northwest"
}

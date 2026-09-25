package com.climatact.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.platform.LocalContext
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
    val hourlyRainProbability: List<Int>,
    val uvIndex: Double,
    val cloudCover: Int,
    val dewPoint: Double,
    val visibility: Int,
    val sunrise: String,
    val sunset: String,
    val daily: List<DailyForecast>,
    val airQuality: AirQualityData
)

private data class AirQualityData(
    val aqi: Int,
    val pm25: Double,
    val pm10: Double
)

private data class DailyForecast(
    val date: String,
    val weatherCode: Int,
    val maxTemp: Double,
    val minTemp: Double,
    val rainProbability: Int
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
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<Place>>(emptyList()) }
    var favorites by remember { mutableStateOf<List<Place>>(emptyList()) }
    var showFavorites by remember { mutableStateOf(false) }

    fun saveFavorite(place: Place) {
        favorites = (favorites.filterNot { it.name == place.name } + place).takeLast(6)
    }

    fun loadWeather(city: String, selected: Place? = null) {
        loading = true
        error = null
        Thread {
            try {
                val place = selected ?: searchPlace(city)
                    ?: throw IllegalArgumentException("Location not found")
                val result = fetchWeather(place)
                runOnUiThread {
                    weather = result
                    query = result.city
                    suggestions = emptyList()
                    loading = false
                    saveFavorite(place)
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

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getLastKnownLocation(context)?.let { loadWeatherByCoordinates(it.latitude, it.longitude) }
        }
    }

    LaunchedEffect(query) {
        if (query.length >= 2) {
            Thread {
                try {
                    val found = searchPlaces(query)
                    runOnUiThread { suggestions = found }
                } catch (_: Exception) {
                    runOnUiThread { suggestions = emptyList() }
                }
            }.start()
        } else suggestions = emptyList()
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


                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val fine = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            val coarse = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                                getLastKnownLocation(context)?.let { loadWeatherByCoordinates(it.latitude, it.longitude) }
                            } else {
                                locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.MyLocation, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Current location")
                    }
                    OutlinedButton(onClick = { showFavorites = !showFavorites }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Star, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Favorites")
                    }
                }

                if (suggestions.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF10232E), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp)) {
                            suggestions.forEach { place ->
                                TextButton(onClick = { loadWeather(place.name, place) }, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(place.name, fontWeight = FontWeight.SemiBold)
                                        Text(place.country, color = Color(0xFF8EA5B4), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                if (showFavorites && favorites.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF10232E), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp)) {
                            favorites.reversed().forEach { place ->
                                TextButton(onClick = { showFavorites = false; loadWeather(place.name, place) }, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(place.name, fontWeight = FontWeight.SemiBold)
                                        Text(place.country, color = Color(0xFF8EA5B4))
                                    }
                                }
                            }
                        }
                    }
                }

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
    WeatherHero(data)
    InfoCard("NOW • ${data.city.uppercase(Locale.getDefault())}", "${data.temperature.roundToInt()}°", "${weatherDescription(data.weatherCode)} • Feels like ${data.apparent.roundToInt()}°", weatherIcon(data.weatherCode))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard("HUMIDITY", "${data.humidity}%", Icons.Default.WaterDrop, Modifier.weight(1f))
        MetricCard("WIND", "${data.wind.roundToInt()} km/h", Icons.Default.Air, Modifier.weight(1f))
    }
    InfoCard("PRESSURE", "${data.pressure.roundToInt()} hPa", "Sea-level atmospheric pressure", Icons.Default.Speed)

    Text("AIR QUALITY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    InfoCard("AIR QUALITY INDEX", "${data.airQuality.aqi}", "PM2.5 ${data.airQuality.pm25.roundToInt()} µg/m³ • PM10 ${data.airQuality.pm10.roundToInt()} µg/m³", Icons.Default.Air)

    Text("ATMOSPHERIC DETAILS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard("UV INDEX", String.format(Locale.US, "%.1f", data.uvIndex), Icons.Default.WbSunny, Modifier.weight(1f))
        MetricCard("CLOUD COVER", "${data.cloudCover}%", Icons.Default.Cloud, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard("DEW POINT", "${data.dewPoint.roundToInt()}°C", Icons.Default.WaterDrop, Modifier.weight(1f))
        MetricCard("VISIBILITY", "${(data.visibility / 1000.0).roundToInt()} km", Icons.Default.Visibility, Modifier.weight(1f))
    }

    Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF10232E), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("SUNRISE", color = Color(0xFF8EA5B4), style = MaterialTheme.typography.labelMedium)
                Text(formatClock(data.sunrise), fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SUNSET", color = Color(0xFF8EA5B4), style = MaterialTheme.typography.labelMedium)
                Text(formatClock(data.sunset), fontWeight = FontWeight.Bold)
            }
        }
    }

    Text("NEXT 12 HOURS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    TemperatureChart(data.hourlyTemperatures.take(12))

    Text("7 DAY OUTLOOK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    DailyForecastList(data.daily)
}


@Composable
private fun WeatherHero(data: WeatherData) {
    val transition = rememberInfiniteTransition(label = "sky")
    val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    val isNight = try {
        val now = java.time.LocalTime.now()
        val sunrise = java.time.LocalTime.parse(data.sunrise.substringAfter("T").take(5))
        val sunset = java.time.LocalTime.parse(data.sunset.substringAfter("T").take(5))
        now.isBefore(sunrise) || now.isAfter(sunset)
    } catch (_: Exception) { false }

    Surface(shape = RoundedCornerShape(32.dp), color = if (isNight) Color(0xFF111A3A) else Color(0xFF123B56), modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Box {
            Canvas(Modifier.fillMaxSize()) {
                if (isNight) {
                    for (i in 0 until 30) {
                        val x = (i * 71f + drift * 40f) % size.width
                        val y = (i * 37f) % size.height
                        drawCircle(Color(0xFFDDF5FF).copy(alpha = 0.35f), 1.7f, Offset(x, y))
                    }
                } else {
                    val sunX = size.width * (0.72f + 0.06f * sin(drift * Math.PI).toFloat())
                    drawCircle(Color(0xFFFFD66B).copy(alpha = 0.16f), 64f, Offset(sunX, 58f))
                    drawCircle(Color(0xFFFFD66B), 31f, Offset(sunX, 58f))
                }
                val cloudAlpha = if (data.weatherCode in 1..3 || data.cloudCover > 35) 0.55f else 0.20f
                for (i in 0 until 4) {
                    val x = ((i * 130f + drift * 70f) % (size.width + 150f)) - 75f
                    val y = 115f + (i % 2) * 25f
                    drawCircle(Color.White.copy(alpha = cloudAlpha), 28f, Offset(x, y))
                    drawCircle(Color.White.copy(alpha = cloudAlpha), 38f, Offset(x + 30f, y - 8f))
                    drawCircle(Color.White.copy(alpha = cloudAlpha), 25f, Offset(x + 60f, y + 4f))
                }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(if (isNight) "NIGHT SKY" else "LIVE SKY", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
                Text("${data.city}, ${data.country}", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(weatherDescription(data.weatherCode), color = Color.White.copy(alpha = 0.85f))
            }
            Icon(weatherIcon(data.weatherCode), null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(22.dp).size(48.dp))
        }
    }
}

@Composable
private fun DailyForecastList(days: List<DailyForecast>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF10232E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatDay(day.date),
                        modifier = Modifier.width(72.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        weatherIcon(day.weatherCode),
                        null,
                        tint = Color(0xFF62D8FF),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(weatherDescription(day.weatherCode), style = MaterialTheme.typography.bodyMedium)
                        Text("Rain ${day.rainProbability}%", color = Color(0xFF8EA5B4), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "${day.maxTemp.roundToInt()}° / ${day.minTemp.roundToInt()}°",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatClock(value: String): String =
    value.substringAfter("T", value).take(5)

private fun formatDay(value: String): String =
    try {
        val date = java.time.LocalDate.parse(value)
        date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
    } catch (_: Exception) {
        value.takeLast(5)
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
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f - 4f)
            val storm = weatherCode in 95..99
            val wet = rainProbability >= 20 || weatherCode in 51..82
            val base = when {
                storm -> Color(0xFFB89CFF)
                wet -> Color(0xFF54C8FF)
                else -> Color(0xFF8EE7FF)
            }

            drawCircle(color = base.copy(alpha = 0.035f), radius = 130f * pulse, center = center)

            for (ring in 1..5) {
                val wave = (phase + ring * 0.16f) % 1f
                val radius = 34f + wave * 150f
                drawCircle(
                    color = base.copy(alpha = (0.22f * (1f - wave)).coerceAtLeast(0.025f)),
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                )
            }

            val direction = Math.toRadians(windDirection.toDouble())
            val dx = cos(direction).toFloat()
            val dy = sin(direction).toFloat()

            for (i in 0 until 18) {
                val seed = i / 18f
                val progress = (phase * 1.15f + seed) % 1f
                val side = (i % 6 - 2.5f) * 28f
                val along = progress * size.maxDimension * 1.25f - size.maxDimension * 0.62f
                val px = -dy * side
                val py = dx * side
                val startPoint = Offset(center.x + dx * along + px, center.y + dy * along + py)
                val length = 26f + (i % 4) * 10f
                drawLine(
                    color = base.copy(alpha = 0.10f + (1f - progress) * 0.30f),
                    start = startPoint,
                    end = Offset(startPoint.x + dx * length, startPoint.y + dy * length),
                    strokeWidth = 2.5f
                )
            }

            if (wet) {
                for (i in 0 until 80) {
                    val x = (i * 43f + i * i * 3f) % size.width
                    val fall = (phase * 1.6f + i / 80f) % 1f
                    val y = fall * (size.height + 60f) - 30f
                    val alpha = (0.08f + rainProbability / 380f).coerceAtMost(0.45f)
                    drawLine(
                        color = base.copy(alpha = alpha),
                        start = Offset(x, y),
                        end = Offset(x - 5f, y + 18f),
                        strokeWidth = 1.8f
                    )
                }
            }

            if (storm) {
                val flash = (sin(phase * 6.283f * 3f) + 1f) / 2f
                drawCircle(
                    color = Color(0xFFE9D8FF).copy(alpha = flash * 0.10f),
                    radius = 105f,
                    center = center
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.Center)
                .size((112 * pulse).dp)
                .background(Color(0xFF102B38).copy(alpha = 0.88f), RoundedCornerShape(56.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(weatherIcon(weatherCode), null, tint = Color(0xFFB9F0FF), modifier = Modifier.size(62.dp))
        }

        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                "LIVE ATMOSPHERIC FLOW",
                color = if (weatherCode in 95..99) Color(0xFFB89CFF) else Color(0xFF8EE7FF),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(weatherDescription(weatherCode), color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            color = Color(0xFF071820).copy(alpha = 0.78f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Air, null, tint = if (weatherCode in 95..99) Color(0xFFB89CFF) else Color(0xFF8EE7FF), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(windDirection.toString() + "°", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
@Composable
private fun ClimateScreen(data: WeatherData) {
    Text("CLIMATE CONTEXT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    InfoCard("CURRENT ATMOSPHERE", "${data.temperature.roundToInt()}°C", "Weather is an instantaneous state, not a climate trend.", Icons.Default.Timeline)
    InfoCard("HUMIDITY", "${data.humidity}%", "Relative humidity at 2 m", Icons.Default.WaterDrop)
    InfoCard("PRECIPITATION SIGNAL", "${data.precipitationProbability}%", "Short-term atmospheric probability, not a climate indicator.", Icons.Default.Cloud)
}


private fun searchPlaces(name: String): List<Place> {
    val encoded = URLEncoder.encode(name.trim(), "UTF-8")
    val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json")
    val results = getJson(url).optJSONArray("results") ?: return emptyList()
    return (0 until results.length()).map {
        val item = results.getJSONObject(it)
        Place(item.optString("name", name), item.getDouble("latitude"), item.getDouble("longitude"), item.optString("country", ""))
    }
}

private fun searchPlace(name: String): Place? = searchPlaces(name).firstOrNull()

private fun getLastKnownLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return try {
        manager.getProviders(true).mapNotNull { provider ->
            try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
        }.maxByOrNull { it.time }
    } catch (_: Exception) { null }
}

private fun loadWeatherByCoordinates(latitude: Double, longitude: Double) {
    Thread {
        try {
            val place = Place("Current location", latitude, longitude, "")
            val result = fetchWeather(place)
            runOnUiThread {
                weather = result
                query = result.city
                suggestions = emptyList()
                loading = false
                error = null
            }
        } catch (e: Exception) {
            runOnUiThread { error = e.message ?: "Unable to load current location"; loading = false }
        }
    }.start()
}

private fun fetchAirQuality(latitude: Double, longitude: Double): AirQualityData {
    return try {
        val url = URL("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$latitude&longitude=$longitude&current=us_aqi,pm2_5,pm10&timezone=auto")
        val current = getJson(url).getJSONObject("current")
        AirQualityData(current.optDouble("us_aqi", 0.0).roundToInt(), current.optDouble("pm2_5", 0.0), current.optDouble("pm10", 0.0))
    } catch (_: Exception) {
        AirQualityData(0, 0.0, 0.0)
    }
}

private fun fetchWeather(place: Place): WeatherData {
    val url = URL(
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,pressure_msl,wind_speed_10m,wind_direction_10m,uv_index,cloud_cover,dew_point_2m,visibility" +
            "&hourly=temperature_2m,precipitation_probability" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
            "&forecast_days=7&timezone=auto"
    )
    val json = getJson(url)
    val current = json.getJSONObject("current")
    val hourly = json.getJSONObject("hourly")
    val timesArray = hourly.getJSONArray("time")
    val tempArray = hourly.getJSONArray("temperature_2m")
    val rainArray = hourly.getJSONArray("precipitation_probability")
    val daily = json.getJSONObject("daily")
    val dailyDates = daily.getJSONArray("time")
    val dailyCodes = daily.getJSONArray("weather_code")
    val dailyMax = daily.getJSONArray("temperature_2m_max")
    val dailyMin = daily.getJSONArray("temperature_2m_min")
    val dailyRain = daily.getJSONArray("precipitation_probability_max")
    val sunriseArray = daily.getJSONArray("sunrise")
    val sunsetArray = daily.getJSONArray("sunset")

    val times = mutableListOf<String>()
    val temps = mutableListOf<Double>()
    val rain = mutableListOf<Int>()
    for (i in 0 until minOf(24, timesArray.length())) {
        times += timesArray.getString(i)
        temps += tempArray.getDouble(i)
        rain += rainArray.getInt(i)
    }

    val dailyForecasts = mutableListOf<DailyForecast>()
    for (i in 0 until minOf(7, dailyDates.length())) {
        dailyForecasts += DailyForecast(
            date = dailyDates.getString(i),
            weatherCode = dailyCodes.getInt(i),
            maxTemp = dailyMax.getDouble(i),
            minTemp = dailyMin.getDouble(i),
            rainProbability = dailyRain.getInt(i)
        )
    }

    val air = fetchAirQuality(place.latitude, place.longitude)

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
        hourlyRainProbability = rain,
        uvIndex = current.optDouble("uv_index", 0.0),
        cloudCover = current.optInt("cloud_cover", 0),
        dewPoint = current.optDouble("dew_point_2m", 0.0),
        visibility = current.optInt("visibility", 0),
        sunrise = sunriseArray.optString(0, ""),
        sunset = sunsetArray.optString(0, ""),
        daily = dailyForecasts,
        airQuality = air
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

package com.ayush.climatact

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ClimaTactApp() }
    }
}

@Composable
fun ClimaTactApp() {
    var selected by remember { mutableIntStateOf(0) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7DD3FC),
            secondary = Color(0xFFB794F4),
            background = Color(0xFF06111D),
            surface = Color(0xFF0D1D2A)
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF06111D), Color(0xFF12364A), Color(0xFF07111B))
                    )
                )
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = { ClimaHeader() },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xEE07121D)) {
                        val tabs = listOf(
                            "Weather" to Icons.Default.WbSunny,
                            "Visualizer" to Icons.Default.Radar,
                            "Climate" to Icons.Default.Timeline
                        )
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { selected = index },
                                icon = { Icon(tab.second, contentDescription = tab.first) },
                                label = { Text(tab.first) }
                            )
                        }
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFF7DD3FC))
                            Spacer(Modifier.width(5.dp))
                            Text("Lucknow, India", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text("LIVE", color = Color(0xFF7DD3FC))
                        }
                    }

                    when (selected) {
                        0 -> {
                            item { CurrentWeatherCard() }
                            item { QuickMetrics() }
                            item { ForecastCard() }
                            item { AtmosphereNote() }
                        }
                        1 -> item { VisualizerCard() }
                        2 -> item { ClimateCard() }
                    }
                }
            }
        }
    }
}

@Composable
fun ClimaHeader() {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF12344A)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.WbSunny, null, tint = Color(0xFF7DD3FC))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Clima-Tact", fontWeight = FontWeight.Bold)
                Text(
                    "LIVE ATMOSPHERE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF88AABD)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(Icons.Default.Refresh, null)
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search a city") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun CurrentWeatherCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color(0x66506D80)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("NOW", color = Color(0xFF9BBACA))
                Text(
                    "28°",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Light
                )
                Text(
                    "Partly cloudy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Feels like 30°  ·  Visibility 8 km",
                    color = Color(0xFFB5C8D2)
                )
            }

            Icon(
                Icons.Default.CloudQueue,
                null,
                modifier = Modifier.size(78.dp),
                tint = Color(0xFFBCEBFA)
            )
        }
    }
}

@Composable
fun QuickMetrics() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Metric("Humidity", "68%", Icons.Default.WaterDrop)
        Metric("Wind", "12 km/h", Icons.Default.Air)
        Metric("Pressure", "1008 hPa", Icons.Default.Speed)
    }
}

@Composable
fun Metric(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x44304A5C)
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = Color(0xFF7DD3FC))
            Spacer(Modifier.height(5.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8EA9B8))
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ForecastCard() {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(26.dp),
        color = Color(0x44304A5C)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("7 DAY OUTLOOK", color = Color(0xFF8AA7B6))
            Text(
                "Next on the horizon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            listOf(
                "Today" to "28° / 21°",
                "Sat" to "29° / 22°",
                "Sun" to "30° / 22°",
                "Mon" to "27° / 21°",
                "Tue" to "26° / 20°"
            ).forEachIndexed { index, day ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(day.first, Modifier.width(58.dp))
                    Icon(
                        if (index == 0) Icons.Default.CloudQueue else Icons.Default.WbSunny,
                        null,
                        Modifier.size(22.dp),
                        tint = Color(0xFF98D4EC)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(day.second, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AtmosphereNote() {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(25.dp),
        color = Color(0x552D2345)
    ) {
        Row(Modifier.padding(18.dp)) {
            Icon(Icons.Default.Visibility, null, tint = Color(0xFFB794F4))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("ATMOSPHERE NOTE", color = Color(0xFFB9A6DD))
                Text(
                    "Moist air is dominating the local profile.",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Clima-Tact separates present weather from long-term climate context.",
                    color = Color(0xFFAABBC6),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun VisualizerCard() {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(30.dp),
        color = Color(0x55405B6C)
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("ATMOSPHERIC VISUALIZER", color = Color(0xFF88B8CE))
            Text(
                "Live field",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0x7745C8E8), Color(0x22245C72), Color.Transparent)
                        ),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    null,
                    modifier = Modifier.size(72.dp),
                    tint = Color(0xFF9BE8FF)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Designed for precipitation, satellite, cloud and wind visualization layers.",
                color = Color(0xFFB0C1CB)
            )
        }
    }
}

@Composable
fun ClimateCard() {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(30.dp),
        color = Color(0x443A435A)
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, null, tint = Color(0xFFA8DCF0))
                Spacer(Modifier.width(9.dp))
                Text(
                    "CLIMATE CONTEXT",
                    color = Color(0xFF88AFC1),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "A climate view belongs to years, not hours.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Clima-Tact will eventually connect historical normals, anomalies, trends and environmental indicators to the live weather experience.",
                color = Color(0xFFADBDC7)
            )
        }
    }
}

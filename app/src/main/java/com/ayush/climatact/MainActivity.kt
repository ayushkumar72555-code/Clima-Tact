package com.climatact.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClimaTactApp() }
    }
}

@Composable
fun ClimaTactApp() {
    var screen by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF081018), primary = Color(0xFF62D8FF))) {
        Scaffold(
            containerColor = Color(0xFF081018),
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0C161F)) {
                    NavigationBarItem(screen == 0, { screen = 0 }, { Icon(Icons.Default.WbSunny, null) }, { Text("Weather") })
                    NavigationBarItem(screen == 1, { screen = 1 }, { Icon(Icons.Default.Radar, null) }, { Text("Atmosphere") })
                    NavigationBarItem(screen == 2, { screen = 2 }, { Icon(Icons.Default.Timeline, null) }, { Text("Climate") })
                }
            }
        ) { p ->
            Column(Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("CLIMA-TACT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("LIVE WEATHER / ATMOSPHERE", color = Color(0xFF8EA5B4), style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(value = "Lucknow", onValueChange = {}, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Location") }, singleLine = true)
                when (screen) {
                    0 -> WeatherScreen()
                    1 -> AtmosphereScreen()
                    else -> ClimateScreen()
                }
            }
        }
    }
}

@Composable
fun InfoCard(title: String, value: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = Color(0xFF122530), modifier = Modifier.fillMaxWidth()) {
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
fun WeatherScreen() {
    InfoCard("NOW • LUCKNOW", "28°", "Partly cloudy • Feels like 30°", Icons.Default.CloudQueue)
    InfoCard("HUMIDITY", "68%", "Moist air", Icons.Default.WaterDrop)
    InfoCard("WIND", "12 km/h", "East-northeast", Icons.Default.Air)
    InfoCard("PRESSURE", "1008 hPa", "Stable", Icons.Default.Speed)
    InfoCard("NEXT 12 HOURS", "23°–28°", "Rain probability rises overnight", Icons.Default.Schedule)
}

@Composable
fun AtmosphereScreen() {
    Text("ATMOSPHERIC VISUALIZER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Surface(shape = MaterialTheme.shapes.extraLarge, color = Color(0xFF0E2633), modifier = Modifier.fillMaxWidth().height(360.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Radar, null, tint = Color(0xFF62D8FF), modifier = Modifier.size(140.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text("Wind • moisture • precipitation", fontWeight = FontWeight.Bold)
                Text("Animated visualization layer", color = Color(0xFF8EA5B4))
            }
        }
    }
    InfoCard("RAIN PROBABILITY", "42%", "Rising after 22:00", Icons.Default.WaterDrop)
}

@Composable
fun ClimateScreen() {
    Text("CLIMATE CONTEXT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    InfoCard("THE LONG VIEW", "Weather is now.", "Climate is the pattern across years and decades.", Icons.Default.Timeline)
    InfoCard("ANOMALIES", "+1.2°C", "Prototype long-term indicator", Icons.Default.ShowChart)
    InfoCard("AIR QUALITY", "Moderate", "PM2.5 trend placeholder", Icons.Default.Cloud)
}

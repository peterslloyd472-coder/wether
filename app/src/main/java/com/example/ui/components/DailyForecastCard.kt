package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyForecast
import com.example.model.TemperatureUnit
import com.example.model.WeatherUtils
import com.example.ui.theme.WeatherRain

@Composable
fun DailyForecastCard(
    dailyList: List<DailyForecast>,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    if (dailyList.isEmpty()) return

    // Show up to 5 days per user request
    val displayDays = dailyList.take(5)
    val overallMin = displayDays.minOfOrNull { it.minTempC } ?: 0.0
    val overallMax = displayDays.maxOfOrNull { it.maxTempC } ?: 40.0
    val tempSpan = (overallMax - overallMin).coerceAtLeast(1.0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("five_day_forecast_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = "Forecast Calendar",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "5-DAY FORECAST",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            displayDays.forEachIndexed { index, day ->
                DailyForecastRow(
                    forecast = day,
                    unit = unit,
                    overallMin = overallMin,
                    overallMax = overallMax,
                    tempSpan = tempSpan
                )
                if (index < displayDays.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastRow(
    forecast: DailyForecast,
    unit: TemperatureUnit,
    overallMin: Double,
    overallMax: Double,
    tempSpan: Double
) {
    val iconInfo = getWeatherIcon(forecast.weatherCode, isDay = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Day Name
        Text(
            text = forecast.dayOfWeek,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )

        // Weather Icon & rain chance
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.width(80.dp)
        ) {
            Icon(
                imageVector = iconInfo.icon,
                contentDescription = forecast.conditionText,
                tint = iconInfo.tint,
                modifier = Modifier.size(24.dp)
            )
            if (forecast.precipitationProb > 10) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${forecast.precipitationProb}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = WeatherRain
                )
            }
        }

        // Min Temp
        Text(
            text = WeatherUtils.formatTemperature(forecast.minTempC, unit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(42.dp)
        )

        // Range bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val leftFraction = ((forecast.minTempC - overallMin) / tempSpan).toFloat().coerceIn(0f, 0.9f)
            val rightFraction = ((forecast.maxTempC - overallMin) / tempSpan).toFloat().coerceIn(leftFraction + 0.1f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = rightFraction)
                    .padding(start = (leftFraction * 100).dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Max Temp
        Text(
            text = WeatherUtils.formatTemperature(forecast.maxTempC, unit),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(42.dp)
        )
    }
}

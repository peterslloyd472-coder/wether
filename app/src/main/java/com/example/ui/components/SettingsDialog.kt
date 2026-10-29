package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AppEdition
import com.example.model.TemperatureUnit
import com.example.model.ThemeMode
import com.example.model.WeatherProvider

@Composable
fun SettingsDialog(
    currentEdition: AppEdition,
    currentUnit: TemperatureUnit,
    currentTheme: ThemeMode,
    currentProvider: WeatherProvider,
    initialOpenWeatherKey: String,
    initialWeatherApiKey: String,
    onEditionChange: (AppEdition) -> Unit,
    onSaveSettings: (
        unit: TemperatureUnit,
        theme: ThemeMode,
        provider: WeatherProvider,
        openWeatherKey: String,
        weatherApiKey: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedEdition by remember { mutableStateOf(currentEdition) }
    var selectedUnit by remember { mutableStateOf(currentUnit) }
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var openWeatherKey by remember { mutableStateOf(initialOpenWeatherKey) }
    var weatherApiKey by remember { mutableStateOf(initialWeatherApiKey) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Application Edition (F-Droid FOSS vs Google)
                Text(
                    text = "APPLICATION EDITION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                AppEdition.entries.forEach { edition ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedEdition = edition }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedEdition == edition,
                            onClick = { selectedEdition = edition }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = edition.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = edition.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Temperature Units
                Text(
                    text = "TEMPERATURE UNIT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TemperatureUnit.entries.forEach { unit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedUnit = unit }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedUnit == unit,
                                onClick = { selectedUnit = unit }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${unit.label} (${unit.symbol})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Theme Mode
                Text(
                    text = "THEME MODE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                ThemeMode.entries.forEach { theme ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTheme = theme }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedTheme == theme,
                            onClick = { selectedTheme = theme }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = theme.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // Weather Provider Selection
                Text(
                    text = "WEATHER DATA PROVIDER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                WeatherProvider.entries.forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedProvider = provider }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (selectedProvider == WeatherProvider.OPEN_WEATHER_MAP) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = openWeatherKey,
                        onValueChange = { openWeatherKey = it },
                        label = { Text("OpenWeatherMap API Key") },
                        placeholder = { Text("Enter your OWM appid") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedProvider == WeatherProvider.WEATHER_API) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = weatherApiKey,
                        onValueChange = { weatherApiKey = it },
                        label = { Text("WeatherAPI.com Key") },
                        placeholder = { Text("Enter your WeatherAPI key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onEditionChange(selectedEdition)
                            onSaveSettings(
                                selectedUnit,
                                selectedTheme,
                                selectedProvider,
                                openWeatherKey,
                                weatherApiKey
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

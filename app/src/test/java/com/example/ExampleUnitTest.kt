package com.example

import com.example.model.TemperatureUnit
import com.example.model.UserSession
import com.example.model.WeatherUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCelsiusToFahrenheitConversion() {
        val fahrenheit = WeatherUtils.convertCelsiusToUnit(0.0, TemperatureUnit.FAHRENHEIT)
        assertEquals(32.0, fahrenheit, 0.01)

        val fahrenheit100 = WeatherUtils.convertCelsiusToUnit(100.0, TemperatureUnit.FAHRENHEIT)
        assertEquals(212.0, fahrenheit100, 0.01)

        val celsiusFormat = WeatherUtils.formatTemperature(20.4, TemperatureUnit.CELSIUS)
        assertEquals("20°C", celsiusFormat)

        val fahrenheitFormat = WeatherUtils.formatTemperature(20.0, TemperatureUnit.FAHRENHEIT)
        assertEquals("68°F", fahrenheitFormat)
    }

    @Test
    fun testSpeedConversion() {
        val kmh = WeatherUtils.formatSpeed(10.0, TemperatureUnit.CELSIUS)
        assertEquals("10 km/h", kmh)

        val mph = WeatherUtils.formatSpeed(10.0, TemperatureUnit.FAHRENHEIT)
        assertEquals("6 mph", mph)
    }

    @Test
    fun testWeatherCodeMapping() {
        assertEquals("Clear Sky", WeatherUtils.mapWmoCodeToText(0))
        assertEquals("Thunderstorm", WeatherUtils.mapWmoCodeToText(95))
        assertEquals("Moderate Rain", WeatherUtils.mapWmoCodeToText(61))
    }

    @Test
    fun testAdminEmailRecognition() {
        assertTrue(UserSession.isEmailAdmin("peterslloyd472@gmail.com"))
        assertTrue(UserSession.isEmailAdmin("  PETERSLLOYD472@GMAIL.COM  "))
        assertFalse(UserSession.isEmailAdmin("regular.user@example.com"))
    }

    @Test
    fun testUserInitials() {
        val session1 = UserSession("1", "peterslloyd472@gmail.com", "Peters Lloyd", isAdmin = true)
        assertEquals("PL", session1.displayInitials)

        val session2 = UserSession("2", "jane@example.com", "Jane", isAdmin = false)
        assertEquals("JA", session2.displayInitials)
    }
}

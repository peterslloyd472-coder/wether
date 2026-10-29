package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_cities")
data class FavoriteCityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val addedAtMillis: Long = System.currentTimeMillis()
)

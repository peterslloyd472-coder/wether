package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCityDao {
    @Query("SELECT * FROM favorite_cities ORDER BY addedAtMillis DESC")
    fun getAllFavorites(): Flow<List<FavoriteCityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(city: FavoriteCityEntity): Long

    @Delete
    suspend fun deleteFavorite(city: FavoriteCityEntity)

    @Query("DELETE FROM favorite_cities WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_cities WHERE ABS(latitude - :lat) < 0.05 AND ABS(longitude - :lon) < 0.05)")
    fun isFavorite(lat: Double, lon: Double): Flow<Boolean>

    @Query("DELETE FROM favorite_cities WHERE ABS(latitude - :lat) < 0.05 AND ABS(longitude - :lon) < 0.05")
    suspend fun removeByCoordinates(lat: Double, lon: Double)

    @Query("DELETE FROM favorite_cities")
    suspend fun clearAllFavorites()
}

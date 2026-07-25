package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    @Query("SELECT * FROM saved_cities ORDER BY name ASC")
    fun getAllCities(): Flow<List<CityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: CityEntity)

    @Delete
    suspend fun deleteCity(city: CityEntity)

    @Query("SELECT * FROM saved_cities WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCity(): CityEntity?

    @Query("SELECT * FROM saved_cities ORDER BY isDefault DESC, name ASC LIMIT 1")
    suspend fun getPreferredCity(): CityEntity?

    @Query("SELECT * FROM saved_cities ORDER BY name ASC")
    suspend fun getAllCitiesOnce(): List<CityEntity>
}

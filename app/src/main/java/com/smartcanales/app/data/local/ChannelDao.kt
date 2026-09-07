package com.smartcanales.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

	@Query("SELECT * FROM channels ORDER BY name ASC")
	fun observeAll(): Flow<List<Channel>>

	@Query("SELECT * FROM channels ORDER BY name ASC")
	suspend fun getAll(): List<Channel>

	@Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): Channel?

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(channel: Channel): Long

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(channels: List<Channel>)

	@Query("DELETE FROM channels WHERE id = :id")
	suspend fun deleteById(id: Long)

	@Query("DELETE FROM channels")
	suspend fun deleteAll()

	@Query("SELECT COUNT(*) FROM channels")
	suspend fun count(): Int

	@Transaction
	suspend fun replaceAll(channels: List<Channel>) {
		deleteAll()
		if (channels.isNotEmpty()) {
			insertAll(channels)
		}
	}
}

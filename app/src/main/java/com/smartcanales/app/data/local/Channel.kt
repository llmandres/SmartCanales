package com.smartcanales.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val name: String,
	val streamUrl: String,
	val groupTitle: String? = null
)

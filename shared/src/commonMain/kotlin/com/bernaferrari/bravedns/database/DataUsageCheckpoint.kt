package com.bernaferrari.bravedns.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity
data class DataUsageCheckpoint(@PrimaryKey val id: Int = 1, val through: Long)

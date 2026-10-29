package com.qomg.webappcatalogs.webapp

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "web_apps", indices = [Index(value = ["url"], unique = true)])
data class WebApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0
)

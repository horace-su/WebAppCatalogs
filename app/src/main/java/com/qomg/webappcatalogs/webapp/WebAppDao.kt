package com.qomg.webappcatalogs.webapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WebAppDao {
    @Query("SELECT * FROM web_apps ORDER BY lastOpenedAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<WebApp>>

    @Query("SELECT * FROM web_apps WHERE id = :id")
    suspend fun get(id: Long): WebApp?

    @Query("SELECT * FROM web_apps WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): WebApp?

    @Insert
    suspend fun insert(app: WebApp): Long

    @Query("UPDATE web_apps SET lastOpenedAt = :time WHERE id = :id")
    suspend fun markOpened(id: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM web_apps WHERE id = :id")
    suspend fun delete(id: Long)
}

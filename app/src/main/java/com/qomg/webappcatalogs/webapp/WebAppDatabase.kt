package com.qomg.webappcatalogs.webapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WebApp::class], version = 1)
abstract class WebAppDatabase : RoomDatabase() {
    abstract fun webAppDao(): WebAppDao

    companion object {
        @Volatile
        private var INSTANCE: WebAppDatabase? = null

        fun getInstance(context: Context): WebAppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WebAppDatabase::class.java,
                    "webapp_database.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

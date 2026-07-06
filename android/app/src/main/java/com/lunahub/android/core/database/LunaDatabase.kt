package com.lunahub.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DownloadTaskEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LunaDatabase : RoomDatabase() {
    abstract fun downloadTaskDao(): DownloadTaskDao
}

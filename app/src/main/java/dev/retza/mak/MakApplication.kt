package dev.retza.mak

import android.app.Application
import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.data.repository.MakRepository
import dev.retza.mak.data.repository.RoomMakRepository

class MakApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: MakRepository by lazy { RoomMakRepository(database) }
}

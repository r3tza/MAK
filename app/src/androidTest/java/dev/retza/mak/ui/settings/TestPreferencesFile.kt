package dev.retza.mak.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job

/** A Preferences DataStore file for device tests; each [open] reads the same file in a new scope. */
class TestPreferencesFile(context: Context) {
    private val file = File(context.filesDir, "settings-${System.nanoTime()}.preferences_pb")
    private var scope: CoroutineScope? = null

    fun open(): DataStore<Preferences> {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())
        scope = dataStoreScope
        return PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { file })
    }

    /** DataStore releases the file only after its scope completes, so a second [open] waits for it. */
    suspend fun close() {
        scope?.coroutineContext?.job?.cancelAndJoin()
    }

    fun delete() {
        scope?.cancel()
        file.delete()
    }
}

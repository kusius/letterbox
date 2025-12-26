package io.kusius.letterbox.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            producePath().toPath()
        },
    )

expect fun getDataStore(): DataStore<Preferences>

internal object DataStoreConstants {
    const val DATA_STORE_FILE_NAME = "prefs.preferences_pb"
}

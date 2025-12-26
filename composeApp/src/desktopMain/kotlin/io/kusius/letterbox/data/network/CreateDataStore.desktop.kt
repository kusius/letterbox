package io.kusius.letterbox.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.kusius.letterbox.DesktopOS
import io.kusius.letterbox.data.network.DataStoreConstants.DATA_STORE_FILE_NAME
import io.kusius.letterbox.getAppDir
import java.io.File

actual fun getDataStore(): DataStore<Preferences> =
    createDataStore(
        producePath = {
            val file =
                getAppDir().resolve(DATA_STORE_FILE_NAME)

            file.absolutePath
        },
    )

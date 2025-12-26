package io.kusius.letterbox.data.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.kusius.letterbox.data.network.DataStoreConstants.DATA_STORE_FILE_NAME

fun createDataStore(context: Context): DataStore<Preferences> =
    createDataStore {
        context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
    }

class AndroidDataStore private constructor(
    context: Context,
) : DataStore<Preferences> by createDataStore(context) {
    companion object {
        private var instance: AndroidDataStore? = null

        fun initialize(context: Context) {
            instance = AndroidDataStore(context)
        }

        fun getInstance(): AndroidDataStore {
            require(instance != null) {
                "must call initialize first"
            }
            return instance!!
        }
    }
}

actual fun getDataStore(): DataStore<Preferences> = AndroidDataStore.getInstance()

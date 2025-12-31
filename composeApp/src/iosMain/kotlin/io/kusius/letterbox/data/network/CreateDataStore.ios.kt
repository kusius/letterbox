package io.kusius.letterbox.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.kusius.letterbox.data.network.DataStoreConstants.DATA_STORE_FILE_NAME
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun getDataStore(): DataStore<Preferences> = IOSDataStore.getInstance()

@OptIn(ExperimentalForeignApi::class)
private fun createDataStore(): DataStore<Preferences> =
    createDataStore(
        producePath = {
            val documentDirectory: NSURL? =
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = true,
                    error = null,
                )

            requireNotNull(documentDirectory)
                .URLByAppendingPathComponent(DATA_STORE_FILE_NAME, false)!!
                .path!!
        },
    )

class IOSDataStore private constructor() : DataStore<Preferences> by createDataStore() {
    companion object {
        private var instance: IOSDataStore? = null

        fun getInstance(): IOSDataStore {
            if (instance == null) {
                instance = IOSDataStore()
            }
            return instance!!
        }
    }
}

package io.kusius.letterbox.data.persistence

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.kusius.letterbox.Database

class AndroidDriverFactory private constructor(
    context: Context,
) : DriverFactory {
    val driver = AndroidSqliteDriver(Database.Schema, context, "letterbox.db")

    override fun createDriver(): SqlDriver = driver

    companion object {
        private var instance: AndroidDriverFactory? = null

        fun initialize(context: Context) {
            if (instance == null) {
                instance = AndroidDriverFactory(context)
            }
        }

        fun getInstance(): AndroidDriverFactory {
            require(instance != null) {
                "You must call initialize(context) first!"
            }
            return instance!!
        }
    }
}

actual fun getDriverFactory(): DriverFactory = AndroidDriverFactory.getInstance()

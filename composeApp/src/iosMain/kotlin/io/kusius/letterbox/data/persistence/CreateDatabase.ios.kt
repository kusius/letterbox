package io.kusius.letterbox.data.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.kusius.letterbox.Database

actual fun getDriverFactory(): DriverFactory = IOSDriverFactory.getInstance()

class IOSDriverFactory private constructor() : DriverFactory {
    val driver = NativeSqliteDriver(Database.Schema, "letterbox.db")

    override fun createDriver(): SqlDriver = driver

    companion object {
        private var instance: IOSDriverFactory? = null

        fun getInstance(): IOSDriverFactory {
            if (instance == null) {
                instance = IOSDriverFactory()
            }
            return instance!!
        }
    }
}

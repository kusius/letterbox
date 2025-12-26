package io.kusius.letterbox.data.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kusius.letterbox.Database
import java.util.Properties

class DesktopDriverFactory private constructor() : DriverFactory {
    override fun createDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:test.db", Properties(), Database.Schema)
        return driver
    }

    companion object {
        private var instance: DesktopDriverFactory? = null

        fun getInstance(): DesktopDriverFactory {
            if (instance == null) {
                instance = DesktopDriverFactory()
            }
            return instance!!
        }
    }
}

actual fun getDriverFactory(): DriverFactory = DesktopDriverFactory.getInstance()

package io.kusius.letterbox.data.persistence

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.SqlDriver
import io.kusius.letterbox.Database
import io.kusius.letterbox.data.MailSeparatorUseCase.emailSeparatorRegex

interface DriverFactory {
    fun createDriver(): SqlDriver
}

expect fun getDriverFactory(): DriverFactory

class DatabaseProvider private constructor(
    val database: Database,
) {
    companion object {
        private var instance: DatabaseProvider? = null

        fun getInstance(): DatabaseProvider {
            if (instance == null) {
                instance = DatabaseProvider(createDatabase())
            }

            return instance!!
        }
    }
}

internal fun createDatabase(driverFactory: DriverFactory = getDriverFactory()): Database {
    val driver = driverFactory.createDriver()
    val database = Database(driver)
    return database
}

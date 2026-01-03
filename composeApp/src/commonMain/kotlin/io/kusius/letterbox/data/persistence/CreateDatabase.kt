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

    if (Database.Schema.version == 1L) {
        Database.Schema.migrate(
            driver = driver,
            oldVersion = 1,
            newVersion = Database.Schema.version,
            AfterVersion(1) { driver ->
                Migrations.populate1To2(driver, database)
            },
        )
    }

    return database
}

private object Migrations {
    fun populate1To2(
        driver: SqlDriver,
        database: Database,
    ) {
        val allMails = database.mailEntityQueries.selectAll().executeAsList()
        // split already stored emails in format "George <george@mail.com>"
        // and populate the new sender_email and old sender column
        allMails.forEach { oldMail ->
            val match = emailSeparatorRegex.matchEntire(oldMail.sender) ?: return@forEach
            val groups = match.groupValues.takeIf { it.size == 3 } ?: return@forEach

            driver.execute(
                identifier = null,
                sql = "UPDATE MailEntity SET sender = ? WHERE id = ?",
                parameters = 2,
                binders = {
                    bindString(0, groups[1])
                    bindString(1, oldMail.id)
                },
            )

            driver.execute(
                identifier = null,
                sql = "UPDATE MailEntity SET senderEmail = ? WHERE id = ?",
                parameters = 2,
                binders = {
                    bindString(0, groups[2])
                    bindString(1, oldMail.id)
                },
            )
        }
    }
}

package io.kusius.letterbox.data.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kusius.letterbox.Database
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MigrationTest {
    @Test
    fun `Migration(1,2) the sender email should be split into two parts`() =
        runTest {
            val driver = loadOldDb(1)

            val result =
                driver.execute(
                    identifier = null,
                    sql =
                        """
                        INSERT INTO MailEntity(id, title, sender, summary, received_at_unix_millis, is_read)
                        VALUES (?, ?, ?, ?, ?, ?);
                        """.trimIndent(),
                    parameters = 6,
                    binders = {
                        bindString(0, "1")
                        bindString(1, "title")
                        bindString(2, "george <george@mail.com>")
                        bindString(3, "summary")
                        bindLong(4, 1L)
                        bindBoolean(5, false)
                    },
                )

            assertEquals(1L, result.await())

            Database.Schema.migrate(
                driver = driver,
                oldVersion = 1,
                newVersion = 2,
            )

            val db = Database(driver)
            val actual = db.mailEntityQueries.getMail("1").executeAsOne()

            assertNotNull(actual)
            assertEquals("george@mail.com", actual.sender_email)
            assertEquals("george", actual.sender)
        }

    fun loadOldDb(version: Int): JdbcSqliteDriver {
        val tmpFile = File.createTempFile("oldDb", ".db")
        val oldDbBytes = javaClass.getResourceAsStream("/databases/$version.db")!!.readBytes()
        tmpFile.writeBytes(oldDbBytes)
        val driver = JdbcSqliteDriver("jdbc:sqlite:${tmpFile.absolutePath}")
        return driver
    }
}

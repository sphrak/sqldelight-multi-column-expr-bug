package com.example

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.Test
import java.io.File

class MultiColumnTest {

    @Test
    fun `test multi column expr`() {

        val dbFile = File("test.db")
        dbFile.delete()

        val driver = JdbcSqliteDriver("jdbc:sqlite:test.db")
        val db = createTestDb(sqlDriver = driver)

        db.testdbQueries.insert("abc", "hello", 42)
        db.testdbQueries.selectPage("abc", 42)

    }

}
const val versionPragma = "user_version"

fun createTestDb(sqlDriver: SqlDriver): Testdb {

    sqlDriver.use {
        it.execute(null, "PRAGMA foreign_keys=ON", 0)
    }

    return Testdb(driver = sqlDriver).apply {

        val oldVersion: Long = sqlDriver.executeQuery(
            identifier = null,
            sql = "PRAGMA $versionPragma",
            mapper = { cursor ->
                if (cursor.next().value) {
                    QueryResult.Value(cursor.getLong(0) ?: 0L)
                } else {
                    QueryResult.Value(0L)
                }
            },
            0,
            null
        ).value

        println("Db version $oldVersion")
        val newVersion = Testdb.Schema.version

        if (oldVersion == 0L) {
            println("Creating DB version $newVersion!")
            Testdb.Schema.create(sqlDriver)
            sqlDriver.execute(null, "PRAGMA $versionPragma=$newVersion", 0)
        } else if (oldVersion < newVersion) {
            println("Migrating DB from version $oldVersion to $newVersion!")
            Testdb.Schema.migrate(sqlDriver, oldVersion, newVersion)
            sqlDriver.execute(null, "PRAGMA $versionPragma=$newVersion", 0)
        }
    }
}
package com.garun

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    fun init() {
        val dbPath = System.getenv("DB_PATH") ?: "./data/leads.db"
        File(dbPath).parentFile?.mkdirs()

        Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC"
        )
        transaction {
            SchemaUtils.create(Leads)
        }
    }
}
